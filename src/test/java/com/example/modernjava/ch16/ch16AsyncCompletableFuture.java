package com.example.modernjava.ch16;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class ch16AsyncCompletableFuture {

    private List<Shop> shops = Arrays.asList(
            new Shop("BestPrice"),
            new Shop("MyFavoriteShop"),
            new Shop("Inwoo"),
            new Shop("InWoo"),
            new Shop("JIW")
    );

    @Test
    public void asyncFuture() {
        Shop shop = new Shop("BestShop");
        long start = System.nanoTime();
        Future<Double> future = shop.getPriceAsync("myProduct");
        long invocationTime = ((System.nanoTime() - start)) / 1_000_000;
        log.info("Invocation returned after invocationTime: {}", invocationTime);
        try {
            double price = future.get();
            log.info("price: {}", price);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        long retrievalTime = ((System.nanoTime() - start) / 1_000_000);
        log.info("price return after: {}", retrievalTime);
        // invocationTime: 3
        // price: 146.6...
        // after: 1027
    }

    @Test
    public void blockCode() {
        long start = System.nanoTime();
        findPrices("myPhone").forEach(price -> log.info("price: {}", price));
        long end = (System.nanoTime() - start) / 1_000_000;
        log.info("endTime: {}", end);
        // 5035
    }
    private List<String> findPrices(String product) {
        return shops.stream()
                .map(shop -> String.format("%s price is %.3f", shop.getName(), shop.getPrice(product)))
                .collect(Collectors.toList());
    }
    @Test
    public void noneBlockCode() {
        long start = System.nanoTime();
        findPricesParallel("myPhone").forEach(price -> log.info("price: {}", price));
        long end = (System.nanoTime() - start) / 1_000_000;
        log.info("endTime: {}", end);
        // 2017
    }
    private List<String> findPricesParallel(String product) {
        return shops.parallelStream()
                .map(shop -> String.format("%s price is %.3f", shop.getName(), shop.getPrice(product)))
                .collect(Collectors.toList());
    }
    @Test
    public void noneBlockAsyncCode() {
        long start = System.nanoTime();
        findPricesAsync("myPhone").forEach(price -> log.info("price: {}", price));
        long end = (System.nanoTime() - start) / 1_000_000;
        log.info("endTime:{}", end);
        // 2045
    }
    private List<String> findPricesAsync(String product) {
        List<CompletableFuture<String>> priceFutures = shops.stream()
                .map(shop -> CompletableFuture.supplyAsync(() -> String.format("%s price is %.3f", shop.getName(), shop.getPrice(product))))
                .collect(Collectors.toList());

        return priceFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Test
    public void nonBlockAsyncExcutorCode() {
        long start = System.nanoTime();
        findPricesAsyncExecutor("myPhone").forEach(price -> log.info("price: {}", price));
        long end = (System.nanoTime() - start) / 1_000_000;
        log.info("endTime:{}", end);
        // 2016
    }

    private final Executor executor = Executors.newFixedThreadPool(Math.min(shops.size(), 100), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread();
            // 자바에서 일반 스레드가 싱행 중이라면 자바 프로그램은 종료되지 않는다.
            // 반면 데몬 스레드같은경우 자바 프로그램이 종료될때 스레드를 강제로 다운시킬 수 있다.
            t.setDaemon(true);
            return t;
        }
    });

    private List<String> findPricesAsyncExecutor(String product) {
        List<CompletableFuture<String>> priceFutures = shops.stream()
                .map(shop -> CompletableFuture.supplyAsync(() -> String.format("%s price is %.3f", shop.getName(), shop.getPrice(product), executor)))
                .collect(Collectors.toList());

        return priceFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

}
