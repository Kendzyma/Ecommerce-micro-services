package com.example.orderservice.service;

import com.example.orderservice.dto.InventoryResponse;
import com.example.orderservice.dto.OrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderLineItems;
import com.example.orderservice.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
@Transactional
public class OrderService {
     private final OrderRepository orderRepository;
     private final ModelMapper mapper;
     private final WebClient.Builder webClientBuilder;
     public void placeOrder(OrderRequest orderRequest){
          Order order = new Order();
          order.setOrderNumber(UUID.randomUUID().toString());

          List<OrderLineItems> orderLineItems = orderRequest
                  .getOrderLineItemsDtosList().stream()
                  .map(orderLineItemsDto -> mapper.map(orderLineItemsDto,OrderLineItems.class)).collect(Collectors.toList());
          order.setOrderLineItemsList(orderLineItems);

          List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();
          InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                  .uri("http://inventory-service/api/inventory",uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                  .retrieve()
                  .bodyToMono(InventoryResponse[].class)
                  .block();

          boolean allProductsInStock = Arrays.stream(inventoryResponses).allMatch(InventoryResponse::isInStock);

          if(allProductsInStock)  orderRepository.save(order);
          else throw new IllegalArgumentException("Product is not in stock, pls try again later");
     }

}
