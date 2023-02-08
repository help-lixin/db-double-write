package help.lixin.data.sync.service.impl;

import help.lixin.data.sync.entity.Order;
import help.lixin.data.sync.mapper.OrderMapper;
import help.lixin.data.sync.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class OrderService implements IOrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean save(Order order) {
        int insert = orderMapper.insert(order);
        return insert > 0 ? true : false;
    }

    /**
     * @Transactional 标记为只读, 所以, 会在多个slave数据源中获取一个数据源.如果, 没有配置slave, 则选择master.
     * 测试在一个Service里,调多次mapper,查看是否都会SQL进行重写.
     */
    @Override
    public List<Map> selectOrderbyIds(List<Long> orderIds) {
        List<Map> maps1 = orderMapper.selectOrderbyIds(orderIds);

//        List<Long> ids = new ArrayList<>();
//        ids.add(565585451440668672L);
//        ids.add(565585451142873088L);
//
//        List<Map> maps2 = orderMapper.selectOrderbyIds(ids);
        return maps1;
    }

    /**
     * 演示事务以及往master写数据.
     *
     * @param orderId
     * @param userId
     * @param price
     * @param status
     * @return
     * @Transactional(readOnly = false , propagation = Propagation.REQUIRED)
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean save(Long orderId, Long userId, BigDecimal price, String status) {
        int count = orderMapper.insertOrder(orderId, userId, price, status);
        if (count > 0) {
            return true;
        }
        return false;
    }
}