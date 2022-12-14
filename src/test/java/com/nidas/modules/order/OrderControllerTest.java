package com.nidas.modules.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nidas.infra.AbstractContainerBaseTest;
import com.nidas.infra.config.MockMvcTest;
import com.nidas.infra.config.TestConfig;
import com.nidas.infra.config.TestProperties;
import com.nidas.modules.account.Account;
import com.nidas.modules.account.AccountRepository;
import com.nidas.modules.account.Membership;
import com.nidas.modules.account.WithAccount;
import com.nidas.modules.accountevent.event.AccountGradeChangedEvent;
import com.nidas.modules.cart.Cart;
import com.nidas.modules.cart.CartFactory;
import com.nidas.modules.cart.CartRepository;
import com.nidas.modules.order.event.OrderStatusChangedEvent;
import com.nidas.modules.order.form.OrderForm;
import com.nidas.modules.product.Product;
import com.nidas.modules.product.ProductDetails;
import com.nidas.modules.product.ProductFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockMvcTest
@Import({TestConfig.class})
public class OrderControllerTest extends AbstractContainerBaseTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProductFactory productFactory;
    @Autowired private CartFactory cartFactory;
    @Autowired private OrderFactory orderFactory;
    @Autowired private CartRepository cartRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private TestProperties testProperties;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ApplicationEventPublisher eventPublisher;

    private final String TEST_EMAIL = "test@email.com";

    @BeforeEach
    public void setup() {
        reset(eventPublisher);
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? ???????????? ?????????")
    @Test
    public void orderForm() throws Exception {
        Product product = productFactory.createProduct();
        List<Cart> carts = cartFactory.createCarts(product, accountRepository.findByEmail(TEST_EMAIL));
        carts.forEach(cart -> cart.setOrderCheck(true));

        mockMvc.perform(get("/order"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("order/make-order"))
                .andExpect(model().attributeExists("account", "cartList", "deliveryFreeBasis", "deliveryPrice"))
                .andExpect(model().attribute("cartList", carts))
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? ???????????? ????????? - ?????? ????????? ?????? ??????")
    @Test
    public void updateProductForm_no_items() throws Exception {
        mockMvc.perform(get("/order"))
                .andDo(print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? - ??????")
    @Test
    public void order() throws Exception {
        Product product = productFactory.createProduct();
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        List<Cart> carts = cartFactory.createCarts(product, account);

        account.setMileage(5000l);
        carts.forEach(cart -> cart.setOrderCheck(true));
        Long totalPayment = carts.stream().mapToLong(cart -> cart.getProductDetails().getProduct().getDiscountedPrice() * cart.getQuantity()).sum();

        OrderForm orderForm = createOrderForm(carts, account);
        mockMvc.perform(post("/order")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderForm))
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        Order order = orderRepository.findAll().get(0);
        assertNotNull(order);
        assertEquals(order.getTotalPrice(), totalPayment);

        List<OrderDetails> orderDetailsList = order.getOrderDetails();
        assertEquals(orderDetailsList.size(), carts.size());
        for (int i = 0; i < orderDetailsList.size(); i++) { // ????????? ?????? ???????????? ????????? ?????????????????? ??????
            OrderDetails orderDetails = orderDetailsList.get(i);
            Cart cart = carts.get(i);

            assertEquals(orderDetails.getProductDetails(), cart.getProductDetails());
            assertEquals(orderDetails.getQuantity(), cart.getQuantity());
            assertEquals(orderDetails.getUnitPrice(), cart.getProductDetails().getProduct().getDiscountedPrice());
            assertEquals(orderDetails.getStatus(), OrderStatus.PAID);
        }

        assertEquals(account.getMileage(), 0); // ?????? ??? ????????? ?????? ?????? ??????
        assertEquals(account.getTotalOrder(), carts.size());
        assertEquals(account.getTotalPayment(), totalPayment);
        assertEquals(account.getMembership(), Membership.GOLD);
        verify(eventPublisher).publishEvent(any(AccountGradeChangedEvent.class)); // ????????? ?????? ???????????? ???????????? ??????

        assertEquals(product.getEntireSalesQuantity(), carts.stream().mapToInt(Cart::getQuantity).sum()); // ?????? ??? ????????? ?????? ?????? ??????
        assertEquals(product.getEntireSalesAmount(), totalPayment);
        product.getDetails().forEach(productDetails -> assertEquals(productDetails.getStock(), 99));

        assertTrue(cartRepository.findAll().isEmpty()); // ?????? ??? ?????? ?????? ?????? ???????????? ??????
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? - ?????? ?????? ????????? ?????? ??????")
    @Test
    public void order_no_cart_id() throws Exception {
        OrderForm orderForm = createOrderForm(Collections.emptyList(), accountRepository.findByEmail(TEST_EMAIL));
        mockMvc.perform(post("/order")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderForm))
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? - ??????????????? ?????? ????????? ?????? ??????")
    @Test
    public void order_not_ordering_items() throws Exception {
        Product product = productFactory.createProduct();
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        List<Cart> carts = cartFactory.createCarts(product, account);

        carts.forEach(cart -> cart.setOrderCheck(false));

        OrderForm orderForm = createOrderForm(carts, account);
        mockMvc.perform(post("/order")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderForm))
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? - ???????????? ?????? ?????????")
    @Test
    public void order_not_verified() throws Exception {
        Product product = productFactory.createProduct();
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        List<Cart> carts = cartFactory.createCarts(product, account);

        account.setEmailVerified(false);
        carts.forEach(cart -> cart.setOrderCheck(true));

        OrderForm orderForm = createOrderForm(carts, account);
        mockMvc.perform(post("/order")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderForm))
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? - ???????????? ??????")
    @Test
    public void order_not_enough_mileage() throws Exception {
        // ?????? ????????? ??? ?????? ??????
        Product product = productFactory.createProduct();
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        List<Cart> carts = cartFactory.createCarts(product, account);

        carts.forEach(cart -> cart.setOrderCheck(true));

        OrderForm orderForm = createOrderForm(carts, account);
        orderForm.setMileageUsage(5000l);
        mockMvc.perform(post("/order")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderForm))
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        // ???????????? ?????? ?????? ???????????? ??? ??????
        account.setMileage(1000000l);
        orderForm.setMileageUsage(account.getMileage());
        mockMvc.perform(post("/order")
                    .characterEncoding("utf8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderForm))
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? ?????? ???????????? ?????????")
    @Test
    public void orderFinishedForm() throws Exception {
        mockMvc.perform(get("/order/finished"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("order/order-finished"))
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    private OrderForm createOrderForm(List<Cart> carts, Account account) {
        OrderForm orderForm = new OrderForm();
        orderForm.setReceiver(account.getName());
        orderForm.setAddress1(testProperties.getAddress1());
        orderForm.setAddress2(testProperties.getAddress2());
        orderForm.setAddress3(testProperties.getAddress3());
        orderForm.setPhoneNumber(account.getPhoneNumber());
        orderForm.setDeliveryRequest("test");
        orderForm.setMileageUsage(account.getMileage());
        orderForm.setCartIdList(carts.stream().map(Cart::getId).collect(Collectors.toSet()));
        return orderForm;
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? ?????? ???????????? ?????????")
    @Test
    public void getOrderList() throws Exception {
        Product product = productFactory.createProduct();
        Order order = orderFactory.createOrder(product, accountRepository.findByEmail(TEST_EMAIL));

        Map<String, Object> model = mockMvc.perform(get("/mypage/order-list"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("order/order-list"))
                .andExpect(model().attributeExists("account", "orderDetailsList"))
                .andExpect(authenticated().withUsername(TEST_EMAIL))
                .andReturn().getModelAndView().getModel();

        List<OrderDetails> orderDetailsList = (List<OrderDetails>) model.get("orderDetailsList");
        assertEquals(order.getOrderDetails().size(), orderDetailsList.size());
        assertTrue(order.getOrderDetails().containsAll(orderDetailsList));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? ?????? ???????????? ?????????")
    @Test
    public void viewOrderInfo() throws Exception {
        Product product = productFactory.createProduct();
        Order order = orderFactory.createOrder(product, accountRepository.findByEmail(TEST_EMAIL));

        mockMvc.perform(get("/mypage/order/" + order.getOrderNumber()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("order/details"))
                .andExpect(model().attributeExists("account", "order"))
                .andExpect(model().attribute("order", order))
                .andExpect(authenticated().withUsername(TEST_EMAIL));
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? - ??????")
    @Test
    public void cancelOrderDetails() throws Exception {
        Product product = productFactory.createProduct();
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        Order order = orderFactory.createOrder(product, account);
        OrderDetails orderDetails = order.getOrderDetails().get(0);
        ProductDetails productDetails = orderDetails.getProductDetails();

        // ?????? ????????? Account??? Product??? ??????
        order.setMileageUsage(1000l);
        account.setTotalOrder(order.getOrderDetails().size());
        account.setTotalPayment(order.getTotalPrice());
        product.setEntireSalesQuantity(order.getOrderDetails().stream().mapToInt(OrderDetails::getQuantity).sum());
        product.setEntireSalesAmount(order.getTotalPrice());
        productDetails.setStock(productDetails.getStock() - orderDetails.getQuantity());

        // ???????????? ?????? ?????????
        Integer prevTotalOrder = account.getTotalOrder();
        Long prevTotalPayment = account.getTotalPayment();
        Integer prevEntireSalesQuantity = product.getEntireSalesQuantity();
        Long prevEntireSalesAmount = product.getEntireSalesAmount();
        Integer prevStock = productDetails.getStock();

        mockMvc.perform(post("/order/details/" + orderDetails.getId() + "/cancel")
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        long orderDetailsTotalPrice = (long) orderDetails.getUnitPrice() * orderDetails.getQuantity();
        long mileage = (long) (order.getMileageUsage() * ((double) orderDetailsTotalPrice / order.getTotalPrice()));

        assertEquals(orderDetails.getStatus(), OrderStatus.CANCELED);
        verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class)); // ?????? ?????? ?????? ???????????? ???????????? ??????

        assertEquals(account.getTotalOrder(), prevTotalOrder - 1);
        assertEquals(account.getTotalPayment(),  prevTotalPayment - orderDetailsTotalPrice);
        assertEquals(account.getMileage(), mileage); // ????????? ????????? ????????? ???????????? ??????

        assertEquals(orderDetails.getProductDetails().getStock(), prevStock + orderDetails.getQuantity());
        assertEquals(product.getEntireSalesQuantity(), prevEntireSalesQuantity - orderDetails.getQuantity());
        assertEquals(product.getEntireSalesAmount(), prevEntireSalesAmount - orderDetailsTotalPrice);
    }

    @WithAccount(TEST_EMAIL)
    @DisplayName("?????? ?????? - ???????????? ????????? ?????? ??????")
    @Test
    public void cancelOrderDetails_not_PAID_status() throws Exception {
        Product product = productFactory.createProduct();
        Account account = accountRepository.findByEmail(TEST_EMAIL);
        Order order = orderFactory.createOrder(product, account);
        OrderDetails orderDetails = order.getOrderDetails().get(0);

        orderDetails.setStatus(OrderStatus.PRODUCT_READY);
        mockMvc.perform(post("/order/details/" + orderDetails.getId() + "/cancel")
                    .with(csrf()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(authenticated().withUsername(TEST_EMAIL));

        assertNotEquals(orderDetails.getStatus(), OrderStatus.CANCELED);
    }

}
