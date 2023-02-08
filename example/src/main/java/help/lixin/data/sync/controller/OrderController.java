package help.lixin.data.sync.controller;

import help.lixin.data.sync.entity.Order;
import help.lixin.data.sync.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private IOrderService orderService;

    /**
     * 演示query
     *
     * @return
     */
    @GetMapping("/list")
    public List<Map> list() {
        List<Long> ids = new ArrayList<>();
        ids.add(620244932388454400L);
        ids.add(620244932535255040L);
        List<Map> maps = orderService.selectOrderbyIds(ids);
        return maps;
    }

    /**
     * 演示master插入数据.
     *
     * @param orderId
     * @param userId
     * @param price
     * @param status
     * @return
     */
    @PostMapping("/save/{orderId}/{userId}/{price}/{status}")
    public String save(@PathVariable("orderId") Long orderId, @PathVariable("userId") Long userId, @PathVariable("price") BigDecimal price, @PathVariable("status") String status) {
        boolean save = orderService.save(orderId, userId, price, status);
        return "SUCCESS";
    }

    @PostMapping("/save2")
    public String save() {
        Long orderId = 500L;
        Long userId = 1000L;
        BigDecimal price = new BigDecimal("100");
        String status = "SUCCESS";

        Order order = new Order();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setPrice(price);
        order.setStatus(status);

        boolean save = orderService.save(order);
        return "SUCCESS";
    }

}