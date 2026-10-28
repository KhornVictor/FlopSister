# Task 4 — REST API Session

## 1. Start the Spring Boot Application

Run the following command from the project root in the VS Code terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

The application runs at `http://localhost:8080`.

Keep this terminal running and open a second terminal to execute the API requests.

## 2. Get All Products

**Request**

```powershell
curl.exe -i http://localhost:8080/api/products
```

**Expected result:** `200 OK`

Returns the list of available products.

## 3. Search Products by Name

**Request**

```powershell
curl.exe -i "http://localhost:8080/api/products?q=mouse"
```

**Expected result:** `200 OK`

Returns products whose names contain the search keyword, ignoring uppercase and lowercase differences.

## 4. Get a Product by ID

**Request**

```powershell
curl.exe -i http://localhost:8080/api/products/1
```

**Expected result:** `200 OK`

Example response:

```json
{
  "id": 1,
  "name": "USB-C cable",
  "price": 7.90,
  "stock": 25
}
```

## 5. Create a Product

**Request**

```powershell
curl.exe -i -X POST "http://localhost:8080/api/products" -H "Content-Type: application/json" -d '{\"name\":\"Headphones\",\"price\":35.50,\"stock\":12}'
```

**Actual result:** `201 Created`

The response contains the created product and a `Location` header identifying its resource URL.

Example:

```text
Location: /api/products/4
```

Example response:

```json
{
  "id": 4,
  "name": "Headphones",
  "price": 35.50,
  "stock": 12
}
```

## 6. Update Product Stock

Decrease the stock of product 4 by two units.

**Request**

```powershell
curl.exe -i -X PATCH http://localhost:8080/api/products/4/stock -H "Content-Type: application/json" -d '{\"delta\":-2}'
```

**Actual result:** `200 OK`

The stock decreases from 12 to 10.

Example response:

```json
{
  "id": 4,
  "name": "Headphones",
  "price": 35.50,
  "stock": 10
}
```

## 7. Test Insufficient Stock

Attempt to remove more stock than is available.

**Request**

```powershell
curl.exe -i -X PATCH http://localhost:8080/api/products/4/stock -H "Content-Type: application/json" -d '{\"delta\":-100}'
```

**Actual result:** `409 Conflict`

The response explains that the requested stock reduction is not possible because only 10 units are available.

## 8. Test Validation Errors

Submit a product with an empty name and a price below the minimum allowed value.

**Request**

```powershell
curl.exe -i -X POST http://localhost:8080/api/products -H "Content-Type: application/json" -d '{\"name\":\"\",\"price\":0,\"stock\":12}'
```

**Actual result:** `400 Bad Request`

The response includes an `errors` property identifying the invalid fields, including `name` and `price`.

## 9. Test a Not-Found Error

Request a product ID that does not exist.

**Request**

```powershell
curl.exe -i http://localhost:8080/api/products/9999
```

**Expected result:** `404 Not Found`

The response uses Problem Details to describe the missing product.

## 10. Delete a Product

Delete product 4.

**Request**

```powershell
curl.exe -i -X DELETE http://localhost:8080/api/products/4
```

**Actual result:** `204 No Content`

The product is deleted successfully, and the response has no body.

## 11. Verify the Deleted Product

Request the deleted product again.

**Request**

```powershell
curl.exe -i http://localhost:8080/api/products/4
```

**Expected result:** `404 Not Found`

The API reports that the deleted product can no longer be found.

## Conclusion

This API session covers the main REST API operations and error-handling scenarios:

- Retrieving all products and searching by name
- Retrieving a product by ID
- Creating products with `201 Created` and a `Location` header
- Updating stock with `PATCH`
- Deleting products with `204 No Content`
- Handling missing products with `404 Not Found`
- Validating request data with `400 Bad Request`
- Handling insufficient stock with `409 Conflict`

The successful tests confirm that the main product API operations and their corresponding HTTP responses work as expected.
