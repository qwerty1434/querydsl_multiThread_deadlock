package com.example.demo.multiThread;

import com.example.demo.domain.A;
import com.example.demo.domain.B;
import com.example.demo.domain.Dto;
import com.example.demo.service.Service;
import com.querydsl.codegen.ClassPathUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.time.Duration;

@SpringBootTest
@Slf4j
@Transactional
public class deadlock {
    @Autowired
    Service service;

    @Autowired
    EntityManager em;

    @BeforeEach
    void initData(){
        B b = B.builder().id(1L).var1(99).build();
        A a = A.builder().id(1L).var1(1).build();
        b.setA(a);
        a.setB(b);
        em.persist(a);
        em.persist(b);
        em.flush();
        em.clear();
        log.info("Data insert End");

    }

    @Test
    void getDataWithoutOtherThread(){
        Dto basic = service.getDataForTransactionCheckBasicPropagation();
        Dto requiresNew = service.getDataForTransactionCheckRequiresNew();
        Assertions.assertNotNull(basic);
        Assertions.assertNull(requiresNew);
    }

    @Test
    void getDataInMyThread() throws InterruptedException {
        Runnable userA = () -> {
            log.info("thread A start");
            Dto basic = service.getDataForTransactionCheckBasicPropagation();
            Dto requiresNew = service.getDataForTransactionCheckRequiresNew();
            log.info("basic = {}",basic);
            log.info("requiresNew = {}",requiresNew);
        };
        Thread threadA = new Thread(userA);
        threadA.start();
        threadA.join();
    }

    @Test
    void occurDeadlock(){
        Runnable userA = () -> {
            log.info("thread A start");
            Dto dto = service.getAData();
            log.info("dto = {}",dto);
        };
        Thread threadA = new Thread(userA);
        threadA.start();

        Runnable userB = () -> {
            log.info("thread B start");
            Dto dto = service.getBData();
            log.info("dto = {}",dto);

        };
        Thread threadB = new Thread(userB);
        threadB.start();

        Assertions.assertTimeoutPreemptively(Duration.ofMillis(4000),()->{
            threadA.join();
            threadB.join();
        });
    }

    @Test
    void avoidDeadlock() throws IOException {
        ClassPathUtils.scanPackage(Thread.currentThread().getContextClassLoader(), "com.example.demo.domain");
        Runnable userA = () -> {
            log.info("thread A start");
            Dto dto = service.getAData();

            log.info("dto = {}",dto);
        };
        Thread threadA = new Thread(userA);
        threadA.start();

        Runnable userB = () -> {
            log.info("thread B start");
            Dto dto = service.getBData();
            log.info("dto = {}",dto);

        };
        Thread threadB = new Thread(userB);
        threadB.start();

        Assertions.assertTimeoutPreemptively(Duration.ofMillis(4000),()->{
            threadA.join();
            threadB.join();
        });

    }
}
