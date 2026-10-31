// Meylorng's part: Product page controller for list page, form page, and redirect flow.
package edu.itc.shop.web;

import java.util.List;

import edu.itc.shop.config.ShopProperties;
import edu.itc.shop.model.Product;
import edu.itc.shop.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductPageController {
    private static final Logger log = LoggerFactory.getLogger(ProductPageController.class);

    private final ProductService productService;
    private final ShopProperties shopProperties;

    public ProductPageController(@Qualifier("shopProductService") ProductService productService, ShopProperties shopProperties) {
        this.productService = productService;
        this.shopProperties = shopProperties;
    }

    @ModelAttribute("currency")
    public String currency() {
        return shopProperties.currency();
    }

    @GetMapping("/products")
    public String listProducts(@RequestParam(value = "q", required = false) String q, Model model) {
        List<Product> products = productService.findAll(q);
        model.addAttribute("products", products);
        model.addAttribute("query", q == null ? "" : q);
        return "products/list";
    }

    @GetMapping("/products/new")
    public String showProductForm(Model model) {
        if (!model.containsAttribute("product")) {
            model.addAttribute("product", new Product());
        }
        return "products/form";
    }

    @PostMapping("/products")
    public String saveProduct(
            @Valid @ModelAttribute("product") Product product,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "products/form";
        }

        Product saved = productService.create(product);
        redirectAttributes.addFlashAttribute(
                "flashMessage",
                "Product '" + saved.getName() + "' created successfully (gross " + saved.getGross() + ")."
        );

        return "redirect:/products";
    }
}
