package dev.kaiwen.controller.user;

import dev.kaiwen.entity.Category;
import dev.kaiwen.result.Result;
import dev.kaiwen.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Category controller for client side.
 */
@RestController("userCategoryController")
@RequestMapping("/user/category")
@Tag(name = "C端-分类接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * Get categories by type.
     *
     * @param type The category type.
     * @return The list of categories matching the type.
     */
    @GetMapping("/list")
    @Operation(summary = "查询分类")
    public Result<List<Category>> list(Integer type) {
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }
}
