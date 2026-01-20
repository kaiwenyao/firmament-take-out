package dev.kaiwen.result;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 封装分页查询结果.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult {

  // 总记录数
  private long total;

  // 当前页数据集合
  private List<?> records;

}
