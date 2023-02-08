package help.lixin.data.sync.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


import help.lixin.data.sync.entity.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * 插入订单
     *
     * @param price
     * @param userId
     * @param status
     * @return
     */
    @Insert("insert into t_order_1(order_id,user_id,price,status)values(#{orderId},#{userId},#{price},#{status})")
    int insertOrder(@Param("orderId") Long orderId, @Param("userId") Long userId, @Param("price") BigDecimal price, @Param("status") String status);

    List<Map> selectOrderbyIds(@Param("orderIds") List<Long> orderIds);
}