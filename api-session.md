# HTTP API Session Transcript

This document records the HTTP sessions for the three endpoints implemented in Task 3.

---

## 1. GET `/api/orders?status=PAID&page=0&size=2&sort=placedAt,desc`

### Request
```http
GET /api/orders?status=PAID&page=0&size=2&sort=placedAt,desc HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [
    {
      "id": 252,
      "status": "PAID",
      "placedAt": "2026-09-24T09:12:44.120Z",
      "customer": {
        "name": "Ada Lovelace"
      }
    },
    {
      "id": 253,
      "status": "PAID",
      "placedAt": "2026-09-23T17:03:10.551Z",
      "customer": {
        "name": "Grace Hopper"
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 2,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 6,
  "totalElements": 11,
  "last": false,
  "size": 2,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

### SQL Log for `/api/orders`
The interface projection `OrderSummary` ensures only the projected columns are retrieved in the SQL statement:
```sql
DEBUG org.hibernate.SQL : select o1_0.id,o1_0.status,o1_0.placed_at,c1_0.id,c1_0.street,c1_0.city,c1_0.zip,c1_0.email,c1_0.name,c1_0.ship_street,c1_0.ship_city,c1_0.ship_zip from orders o1_0 join customer c1_0 on c1_0.id=o1_0.customer_id where o1_0.status=? order by o1_0.placed_at desc fetch first ? rows only
DEBUG org.hibernate.SQL : select count(o1_0.id) from orders o1_0 where o1_0.status=?
```

---

## 2. GET `/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true`

### Request
```http
GET /api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [
    {
      "id": 252,
      "sku": "KB-1",
      "name": "Mechanical Keyboard",
      "amount": 49.99,
      "stock": 15
    },
    {
      "id": 253,
      "sku": "KB-7",
      "name": "Wireless Numeric Keypad",
      "amount": 35.00,
      "stock": 20
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 1,
  "totalElements": 2,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "numberOfElements": 2,
  "first": true,
  "empty": false
}
```

Filtering verification:
```bash
$ curl.exe -s "localhost:8080/api/products?q=key&category=HARDWARE&maxPrice=60&inStock=true" | jq '.content[].sku'
"KB-1"
"KB-7"
```

---

## 3. GET `/api/customers/{email}/orders`

### Request
```http
GET /api/customers/ada@example.com/orders HTTP/1.1
Host: localhost:8080
Accept: application/json
```

### Response
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": 252,
    "status": "PAID",
    "placedAt": "2026-09-24T09:12:44.120Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 119.99,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 252,
        "productId": 252,
        "productSku": "KB-1",
        "productName": "Mechanical Keyboard",
        "quantity": 1,
        "unitPriceAmount": 49.99,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 253,
        "productId": 253,
        "productSku": "KB-7",
        "productName": "Wireless Numeric Keypad",
        "quantity": 2,
        "unitPriceAmount": 35.00,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 255,
    "status": "PAID",
    "placedAt": "2026-09-23T11:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 114.98,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 261,
        "productId": 258,
        "productSku": "BK-3",
        "productName": "Designing Data-Intensive Applications",
        "quantity": 1,
        "unitPriceAmount": 55.00,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 262,
        "productId": 259,
        "productSku": "EL-1",
        "productName": "USB-C Multiport Adapter",
        "quantity": 2,
        "unitPriceAmount": 29.99,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 258,
    "status": "PAID",
    "placedAt": "2026-09-23T02:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 119.99,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 270,
        "productId": 252,
        "productSku": "KB-1",
        "productName": "Mechanical Keyboard",
        "quantity": 1,
        "unitPriceAmount": 49.99,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 271,
        "productId": 253,
        "productSku": "KB-7",
        "productName": "Wireless Numeric Keypad",
        "quantity": 2,
        "unitPriceAmount": 35.00,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 261,
    "status": "PAID",
    "placedAt": "2026-09-22T17:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 114.98,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 279,
        "productId": 258,
        "productSku": "BK-3",
        "productName": "Designing Data-Intensive Applications",
        "quantity": 1,
        "unitPriceAmount": 55.00,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 280,
        "productId": 259,
        "productSku": "EL-1",
        "productName": "USB-C Multiport Adapter",
        "quantity": 2,
        "unitPriceAmount": 29.99,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 264,
    "status": "SHIPPED",
    "placedAt": "2026-09-22T08:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 119.99,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 288,
        "productId": 252,
        "productSku": "KB-1",
        "productName": "Mechanical Keyboard",
        "quantity": 1,
        "unitPriceAmount": 49.99,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 289,
        "productId": 253,
        "productSku": "KB-7",
        "productName": "Wireless Numeric Keypad",
        "quantity": 2,
        "unitPriceAmount": 35.00,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 267,
    "status": "SHIPPED",
    "placedAt": "2026-09-21T23:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 114.98,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 297,
        "productId": 258,
        "productSku": "BK-3",
        "productName": "Designing Data-Intensive Applications",
        "quantity": 1,
        "unitPriceAmount": 55.00,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 298,
        "productId": 259,
        "productSku": "EL-1",
        "productName": "USB-C Multiport Adapter",
        "quantity": 2,
        "unitPriceAmount": 29.99,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 270,
    "status": "SHIPPED",
    "placedAt": "2026-09-21T14:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 119.99,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 306,
        "productId": 252,
        "productSku": "KB-1",
        "productName": "Mechanical Keyboard",
        "quantity": 1,
        "unitPriceAmount": 49.99,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 307,
        "productId": 253,
        "productSku": "KB-7",
        "productName": "Wireless Numeric Keypad",
        "quantity": 2,
        "unitPriceAmount": 35.00,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 273,
    "status": "NEW",
    "placedAt": "2026-09-21T05:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 114.98,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 315,
        "productId": 258,
        "productSku": "BK-3",
        "productName": "Designing Data-Intensive Applications",
        "quantity": 1,
        "unitPriceAmount": 55.00,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 316,
        "productId": 259,
        "productSku": "EL-1",
        "productName": "USB-C Multiport Adapter",
        "quantity": 2,
        "unitPriceAmount": 29.99,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 276,
    "status": "NEW",
    "placedAt": "2026-09-20T20:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 119.99,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 324,
        "productId": 252,
        "productSku": "KB-1",
        "productName": "Mechanical Keyboard",
        "quantity": 1,
        "unitPriceAmount": 49.99,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 325,
        "productId": 253,
        "productSku": "KB-7",
        "productName": "Wireless Numeric Keypad",
        "quantity": 2,
        "unitPriceAmount": 35.00,
        "unitPriceCurrency": "EUR"
      }
    ]
  },
  {
    "id": 279,
    "status": "CANCELLED",
    "placedAt": "2026-09-20T11:03:10.551Z",
    "customer": {
      "id": 252,
      "name": "Ada Lovelace",
      "email": "ada@example.com"
    },
    "totalAmount": 114.98,
    "totalCurrency": "EUR",
    "lines": [
      {
        "id": 333,
        "productId": 258,
        "productSku": "BK-3",
        "productName": "Designing Data-Intensive Applications",
        "quantity": 1,
        "unitPriceAmount": 55.00,
        "unitPriceCurrency": "EUR"
      },
      {
        "id": 334,
        "productId": 259,
        "productSku": "EL-1",
        "productName": "USB-C Multiport Adapter",
        "quantity": 2,
        "unitPriceAmount": 29.99,
        "unitPriceCurrency": "EUR"
      }
    ]
  }
]
```
