<%-- Meylorng's part: Product form page for adding new product entries. --%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html>
<head>
    <title>New Product</title>
    <link rel="stylesheet" href="/css/site.css">
</head>
<body>
    <h1>Add a product</h1>

    <form:form method="post" modelAttribute="product">
        <form:errors path="*" cssClass="error" element="div"/>

        <label for="name">Name</label>
        <form:input path="name" id="name"/>
        <form:errors path="name" cssClass="error"/>

        <label for="price">Price</label>
        <form:input path="price" id="price"/>
        <form:errors path="price" cssClass="error"/>

        <label for="stock">Stock</label>
        <form:input path="stock" id="stock"/>
        <form:errors path="stock" cssClass="error"/>

        <p>
            <button type="submit">Save</button>
            <a href="/products">Cancel</a>
        </p>
    </form:form>
</body>
</html>
