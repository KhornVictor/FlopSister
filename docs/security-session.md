
# Security Session — Stored XSS Testing

## 1. Objective

Demonstrate stored Cross-Site Scripting (XSS) risks and verify
that the application safely renders user-generated content.

## 2. Test Payload

```html
<img src=x onerror="document.title='pwned'">
```

## 3. API Testing

An authenticated customer submitted the payload using:

POST /api/reviews

The server successfully stored the review.

The public endpoint:

GET /api/products/1/reviews

returned the review as JSON with HTTP 200.

The response used:

- Content-Type: application/json
- X-Content-Type-Options: nosniff

JSON quotation marks were escaped correctly. The HTML
characters remained part of the JSON string.

## 4. Safe Rendering — th:text

The review was displayed using:

```html
<p th:text="${review.content}"></p>
```

Result:
- The HTML payload appeared as literal text.
- No image element was created from the payload.
- The malicious event handler did not execute.

Screenshot: [Insert safe rendering screenshot here]

## 5. Unsafe Rendering — th:utext

For comparison, the template was temporarily changed to:

```html
<p th:utext="${review.content}"></p>
```

Result:
- The browser interpreted the payload as HTML.
- A broken image icon appeared on the page.
- This demonstrated that unescaped user content can
  create real HTML elements.
- The configured Content Security Policy helped block
  execution of the inline event handler.

Screenshot: [Insert unsafe rendering screenshot here]

## 6. Security Conclusion

Using th:text is safer for displaying untrusted user content
because it escapes HTML characters.

Using th:utext with untrusted input introduces an HTML
injection and potential stored XSS vulnerability.

Content Security Policy provides defense in depth, but
does not replace safe output encoding.

The application was restored to th:text after testing.
The unsafe version must not be merged into the final code.
