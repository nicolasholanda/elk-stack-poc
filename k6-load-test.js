import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const requestDuration = new Trend('request_duration');
const successRate = new Rate('success_rate');
const activeUsers = new Gauge('active_users');
const createdUsers = new Counter('created_users');
const createdOrders = new Counter('created_orders');

// Configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp-up: 0 to 10 users over 30s
    { duration: '1m30s', target: 50 }, // Ramp-up: 10 to 50 users over 1m30s
    { duration: '3m', target: 100 },   // Ramp-up: 50 to 100 users over 3m
    { duration: '5m', target: 100 },   // Stay at 100 users for 5m
    { duration: '2m', target: 50 },    // Ramp-down: 100 to 50 users over 2m
    { duration: '30s', target: 0 },    // Ramp-down: 50 to 0 users over 30s
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% of requests < 500ms, 99% < 1s
    http_req_failed: ['rate<0.1'],                    // Error rate < 10%
    errors: ['rate<0.1'],                             // Custom error rate < 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  activeUsers.add(1);

  // Test 1: Create multiple users
  group('User Management', () => {
    const users = [];

    for (let i = 0; i < 3; i++) {
      const userData = {
        name: `Test User ${__VU}-${__ITER}-${i}`,
        email: `test.user.${__VU}.${__ITER}.${i}@example.com`,
        phone: `+55${String(__VU).padStart(11, '0')}`,
      };

      let createUserResponse = http.post(
        `${BASE_URL}/api/users`,
        JSON.stringify(userData),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const userCreated = check(createUserResponse, {
        'User create status is 201': (r) => r.status === 201,
        'User create response is valid JSON': (r) => {
          try {
            JSON.parse(r.body);
            return true;
          } catch {
            return false;
          }
        },
      });

      if (userCreated) {
        const user = JSON.parse(createUserResponse.body);
        users.push(user);
        createdUsers.add(1);
        successRate.add(true);
      } else {
        errorRate.add(true);
        successRate.add(false);
      }

      requestDuration.add(createUserResponse.timings.duration);
      sleep(0.5);
    }

    // Get all users
    group('Get all users', () => {
      let getAllUsersResponse = http.get(`${BASE_URL}/api/users`);

      check(getAllUsersResponse, {
        'Get all users status is 200': (r) => r.status === 200,
        'Get all users returns array': (r) => {
          try {
            const body = JSON.parse(r.body);
            return Array.isArray(body);
          } catch {
            return false;
          }
        },
      });

      requestDuration.add(getAllUsersResponse.timings.duration);
    });

    sleep(1);

    // Get user by ID (using created users)
    if (users.length > 0) {
      group('Get user by ID', () => {
        const randomUser = users[Math.floor(Math.random() * users.length)];

        let getUserResponse = http.get(`${BASE_URL}/api/users/${randomUser.id}`);

        check(getUserResponse, {
          'Get user by ID status is 200': (r) => r.status === 200,
          'Get user by ID returns correct user': (r) => {
            try {
              const body = JSON.parse(r.body);
              return body.id === randomUser.id;
            } catch {
              return false;
            }
          },
        });

        requestDuration.add(getUserResponse.timings.duration);
      });

      sleep(0.5);

      // Get user by email
      group('Get user by email', () => {
        const randomUser = users[Math.floor(Math.random() * users.length)];

        let getUserByEmailResponse = http.get(
          `${BASE_URL}/api/users/email/${encodeURIComponent(randomUser.email)}`
        );

        check(getUserByEmailResponse, {
          'Get user by email status is 200': (r) => r.status === 200,
          'Get user by email returns correct user': (r) => {
            try {
              const body = JSON.parse(r.body);
              return body.email === randomUser.email;
            } catch {
              return false;
            }
          },
        });

        requestDuration.add(getUserByEmailResponse.timings.duration);
      });

      sleep(0.5);

      // Update user
      group('Update user', () => {
        const randomUser = users[Math.floor(Math.random() * users.length)];
        const updatedUserData = {
          name: `Updated User ${__VU}-${__ITER}`,
          email: randomUser.email,
          phone: '+55 (11) 98765-4321',
        };

        let updateUserResponse = http.put(
          `${BASE_URL}/api/users/${randomUser.id}`,
          JSON.stringify(updatedUserData),
          { headers: { 'Content-Type': 'application/json' } }
        );

        check(updateUserResponse, {
          'Update user status is 200': (r) => r.status === 200,
        });

        requestDuration.add(updateUserResponse.timings.duration);
      });

      sleep(0.5);
    }
  });

  // Test 2: Order Management
  group('Order Management', () => {
    // First, get some users to create orders for
    let getAllUsersResponse = http.get(`${BASE_URL}/api/users`);
    let users = [];

    if (getAllUsersResponse.status === 200) {
      try {
        users = JSON.parse(getAllUsersResponse.body);
      } catch {
        console.error('Failed to parse users response');
        return;
      }
    }

    if (users.length === 0) {
      console.warn('No users available for order creation');
      return;
    }

    // Create orders for random users
    for (let i = 0; i < 2; i++) {
      const randomUser = users[Math.floor(Math.random() * users.length)];
      const statuses = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
      const randomStatus = statuses[Math.floor(Math.random() * statuses.length)];

      const orderData = {
        userId: randomUser.id,
        orderNumber: `ORD-${Date.now()}-${__VU}-${i}`,
        status: randomStatus,
        totalAmount: Math.floor(Math.random() * 10000) / 100,
        description: `Test Order ${__VU}-${__ITER}-${i}`,
      };

      let createOrderResponse = http.post(
        `${BASE_URL}/api/orders`,
        JSON.stringify(orderData),
        { headers: { 'Content-Type': 'application/json' } }
      );

      const orderCreated = check(createOrderResponse, {
        'Order create status is 201': (r) => r.status === 201,
        'Order create response is valid JSON': (r) => {
          try {
            JSON.parse(r.body);
            return true;
          } catch {
            return false;
          }
        },
      });

      if (orderCreated) {
        createdOrders.add(1);
        successRate.add(true);
      } else {
        errorRate.add(true);
        successRate.add(false);
      }

      requestDuration.add(createOrderResponse.timings.duration);
      sleep(0.5);
    }

    // Get all orders
    group('Get all orders', () => {
      let getAllOrdersResponse = http.get(`${BASE_URL}/api/orders`);

      check(getAllOrdersResponse, {
        'Get all orders status is 200': (r) => r.status === 200,
        'Get all orders returns array': (r) => {
          try {
            const body = JSON.parse(r.body);
            return Array.isArray(body);
          } catch {
            return false;
          }
        },
      });

      requestDuration.add(getAllOrdersResponse.timings.duration);
    });

    sleep(1);

    // Get orders by user ID
    if (users.length > 0) {
      group('Get orders by user ID', () => {
        const randomUser = users[Math.floor(Math.random() * users.length)];

        let getOrdersByUserResponse = http.get(
          `${BASE_URL}/api/orders/user/${randomUser.id}`
        );

        check(getOrdersByUserResponse, {
          'Get orders by user status is 200': (r) => r.status === 200,
        });

        requestDuration.add(getOrdersByUserResponse.timings.duration);
      });

      sleep(0.5);
    }

    // Get orders by order number
    let getAllOrdersResponse2 = http.get(`${BASE_URL}/api/orders`);
    if (getAllOrdersResponse2.status === 200) {
      try {
        const orders = JSON.parse(getAllOrdersResponse2.body);
        if (orders.length > 0) {
          group('Get order by order number', () => {
            const randomOrder = orders[Math.floor(Math.random() * orders.length)];

            let getOrderByNumberResponse = http.get(
              `${BASE_URL}/api/orders/number/${randomOrder.orderNumber}`
            );

            check(getOrderByNumberResponse, {
              'Get order by number status is 200': (r) => r.status === 200,
            });

            requestDuration.add(getOrderByNumberResponse.timings.duration);
          });

          sleep(0.5);

          // Update order status
          group('Update order status', () => {
            const randomOrder = orders[Math.floor(Math.random() * orders.length)];
            const statuses = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];
            const randomStatus = statuses[Math.floor(Math.random() * statuses.length)];

            let updateStatusResponse = http.put(
              `${BASE_URL}/api/orders/${randomOrder.id}/status?status=${randomStatus}`,
              null,
              { headers: { 'Content-Type': 'application/json' } }
            );

            check(updateStatusResponse, {
              'Update order status is 200': (r) => r.status === 200,
            });

            requestDuration.add(updateStatusResponse.timings.duration);
          });
        }
      } catch {
        console.error('Failed to parse orders response');
      }
    }
  });

  sleep(2);
  activeUsers.add(-1);
}

