package com.example.demo.transaction;

import com.example.demo.domain.A;
import com.example.demo.domain.B;
import com.example.demo.domain.Dto;
import com.example.demo.service.Service;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@SpringBootTest
@Slf4j
@Transactional
public class IsolationProblem {

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

}
