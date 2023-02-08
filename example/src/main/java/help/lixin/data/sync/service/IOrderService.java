package help.lixin.data.sync.service;

import help.lixin.data.sync.entity.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IOrderService {
    List<Map> selectOrderbyIds(List<Long> orderIds);

    boolean save(Long orderId, Long userId, BigDecimal price, String status);

    boolean save(Order order);
}
