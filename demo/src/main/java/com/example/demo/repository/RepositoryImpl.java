package com.example.demo.repository;

import com.example.demo.domain.Dto;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;

import javax.persistence.EntityManager;

import static com.example.demo.domain.QA.a;
import static com.example.demo.domain.QB.b;

@org.springframework.stereotype.Repository
public class RepositoryImpl implements Repository {
    private final JPAQueryFactory queryFactory;

    public RepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Dto findAFetchFirst() {
        return queryFactory
                .select(Projections.constructor(Dto.class,
                        a.id,
                        a.var1
                ))
                .from(a)
                .fetchFirst();
    }

    @Override
    public Dto findBFetchFirst(){
        return queryFactory
                .select(Projections.constructor(Dto.class,
                        b.id,
                        b.var1
                ))
                .from(b)
                .fetchFirst();
    }

}
