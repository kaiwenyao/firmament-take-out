package dev.kaiwen.converter;

import dev.kaiwen.dto.CategoryDto;
import dev.kaiwen.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Category 转换器 使用 MapStruct 自动生成实现类.
 */
@Mapper
public interface CategoryConverter {

  CategoryConverter INSTANCE = Mappers.getMapper(CategoryConverter.class);

  /**
   * DTO -> Entity (用于新增和修改分类).
   *
   * @param categoryDto 分类DTO
   * @return 分类实体
   */
  Category d2e(CategoryDto categoryDto);
}

