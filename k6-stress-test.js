import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge, Histogram } from 'k6/metrics';

// Custom metrics for detailed analysis
const errorRate = new Rate('errors');
const requestDuration = new Trend('request_duration');
const successRate = new Rate('success_rate');
const activeUsers = new Gauge('active_users');
const userCreationTime = new Trend('user_creation_time');
const orderCreationTime = new Trend('order_creation_time');
const userSearchTime = new Trend('user_search_time');
const orderSearchTime = new Trend('order_search_time');
const responseSize = new Histogram('response_size');

// Configuration for stress testing (more aggressive)
export const options = {
  stages: [
    { duration: '1m', target: 20 },    // Ramp-up
    { duration: '3m', target: 100 },   // Ramp-up more aggressively
    { duration: '5m', target: 200 },   // Heavy load
    { duration: '2m', target: 100 },   // Reduce to normal
    { duration: '1m', target: 0 },     // Ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<800', 'p(99)<2000'],
    http_req_failed: ['rate<0.15'],
    errors: ['rate<0.15'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  activeUsers.add(1);

  // Simulate realistic user workflow
  simulateUserWorkflow();

  sleep(2);
  activeUsers.add(-1);
}

function simulateUserWorkflow() {
  // Workflow 1: User creation flow (30% of users)
  if (Math.random() < 0.3) {
    createAndManageUser();
  }

  // Workflow 2: Order management flow (70% of users)
  if (Math.random() < 0.7) {
    browseAndOrderFlow();
  }
}

function createAndManageUser() {
  group('Complete User Lifecycle', () => {
    // Create user
    const userData = {
      name: `Stress Test User ${__VU}-${__ITER}`,
      email: `stress.${__VU}.${__ITER}.${Date.now()}@example.com`,
      phone: `+55 (11) 9999${String(__VU).padStart(4, '0')}`,
    };

    const startTime = new Date();
    let createUserResponse = http.post(
      `${BASE_URL}/api/users`,
      JSON.stringify(userData),
      { headers: { 'Content-Type': 'application/json' } }
    );
    const creationDuration = new Date() - startTime;

    const userCreated = check(createUserResponse, {
      'User creation successful': (r) => r.status === 201,
    });

    if (userCreated) {
      userCreationTime.add(creationDuration);
      successRate.add(true);

      try {
        const user = JSON.parse(createUserResponse.body);
        responseSize.add(createUserResponse.body.length);

        sleep(0.5);

        // Get the created user
        group('Retrieve created user', () => {
          let getResponse = http.get(`${BASE_URL}/api/users/${user.id}`);
          check(getResponse, {
            'User retrieval successful': (r) => r.status === 200,
          });
          responseSize.add(getResponse.body.length);
        });

        sleep(0.5);

        // Update the user
        group('Update user details', () => {
          const updatedData = {
            ...user,
            name: `Updated ${user.name}`,
            phone: '+55 (11) 98765-4321',
          };

          let updateResponse = http.put(
            `${BASE_URL}/api/users/${user.id}`,
            JSON.stringify(updatedData),
            { headers: { 'Content-Type': 'application/json' } }
          );

          check(updateResponse, {
            'User update successful': (r) => r.status === 200,
          });
          responseSize.add(updateResponse.body.length);
        });
      } catch (e) {
        console.error(`Error in user lifecycle: ${e}`);
        errorRate.add(true);
        successRate.add(false);
      }
    } else {
      errorRate.add(true);
      successRate.add(false);
    }
  });
}

function browseAndOrderFlow() {
  group('Browse and Order Flow', () => {
    // Get all users to browse
    group('Browse users', () => {
      const startTime = new Date();
      let getAllUsersResponse = http.get(`${BASE_URL}/api/users`);
      const searchDuration = new Date() - startTime;

      const success = check(getAllUsersResponse, {
        'User list retrieval successful': (r) => r.status === 200,
      });

      if (success) {
        userSearchTime.add(searchDuration);
        successRate.add(true);
        responseSize.add(getAllUsersResponse.body.length);
      } else {
        userSearchTime.add(searchDuration);
        errorRate.add(true);
        successRate.add(false);
      }
    });

    sleep(1);

    // Get all orders to browse
    group('Browse orders', () => {
      const startTime = new Date();
      let getAllOrdersResponse = http.get(`${BASE_URL}/api/orders`);
      const searchDuration = new Date() - startTime;

      const success = check(getAllOrdersResponse, {
        'Order list retrieval successful': (r) => r.status === 200,
      });

      if (success) {
        orderSearchTime.add(searchDuration);
        successRate.add(true);
        responseSize.add(getAllOrdersResponse.body.length);

        // If there are orders, pick one and view details
        try {
          const orders = JSON.parse(getAllOrdersResponse.body);
          if (orders.length > 0) {
            sleep(0.5);
            group('View order details', () => {
              const randomOrder = orders[Math.floor(Math.random() * orders.length)];
              let getOrderResponse = http.get(
                `${BASE_URL}/api/orders/number/${randomOrder.orderNumber}`
              );

              check(getOrderResponse, {
                'Order details retrieval successful': (r) => r.status === 200,
              });
              responseSize.add(getOrderResponse.body.length);
            });
          }
        } catch (e) {
          console.error(`Error parsing orders: ${e}`);
        }
      } else {
        orderSearchTime.add(searchDuration);
        errorRate.add(true);
        successRate.add(false);
      }
    });

    sleep(1);

    // Create an order
    group('Create new order', () => {
      // First get a user
      let usersResponse = http.get(`${BASE_URL}/api/users`);
      try {
        const users = JSON.parse(usersResponse.body);
        if (users.length > 0) {
          const randomUser = users[Math.floor(Math.random() * users.length)];

          const statuses = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'];
          const randomStatus = statuses[Math.floor(Math.random() * statuses.length)];

          const orderData = {
            userId: randomUser.id,
            orderNumber: `STR-${Date.now()}-${__VU}-${__ITER}`,
            status: randomStatus,
            totalAmount: Math.floor(Math.random() * 50000) / 100,
            description: `Order for ${randomUser.name}`,
          };

          const startTime = new Date();
          let createOrderResponse = http.post(
            `${BASE_URL}/api/orders`,
            JSON.stringify(orderData),
            { headers: { 'Content-Type': 'application/json' } }
          );
          const creationDuration = new Date() - startTime;

          const orderCreated = check(createOrderResponse, {
            'Order creation successful': (r) => r.status === 201,
          });

          if (orderCreated) {
            orderCreationTime.add(creationDuration);
            successRate.add(true);
            responseSize.add(createOrderResponse.body.length);

            // After creating, view the order
            sleep(0.5);
            try {
              const order = JSON.parse(createOrderResponse.body);
              group('View newly created order', () => {
                let getOrderResponse = http.get(`${BASE_URL}/api/orders/${order.id}`);
                check(getOrderResponse, {
                  'Newly created order retrieval successful': (r) => r.status === 200,
                });
                responseSize.add(getOrderResponse.body.length);
              });
            } catch (e) {
              console.error(`Error retrieving order: ${e}`);
            }
          } else {
            orderCreationTime.add(creationDuration);
            errorRate.add(true);
            successRate.add(false);
          }
        }
      } catch (e) {
        console.error(`Error in order creation flow: ${e}`);
        errorRate.add(true);
        successRate.add(false);
      }
    });

    sleep(1);

    // Simulate concurrent reads (quick browsing)
    group('Rapid browsing simulation', () => {
      for (let i = 0; i < 3; i++) {
        const endpoint = Math.random() < 0.5 ? 'users' : 'orders';
        let response = http.get(`${BASE_URL}/api/${endpoint}`);

        check(response, {
          'Browse request successful': (r) => r.status === 200,
        });

        responseSize.add(response.body.length);
        sleep(0.2);
      }
    });
  });
}

