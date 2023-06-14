# querydsl_multiThread_deadlock

# 서론
이 글은 프로젝트를 진행하다가 겪은 문제와 그 문제를 해결하는 과정에 대한 글입니다.
<br>
설명에서 사용되는 코드는 프로젝트 코드가 아니라 해당 문제를 설명하기 위해 만든 단순한 예제 코드입니다.
<br>
저는 진행하던 프로젝트의 프론트에서 vue lifecycle의 created시점에 axios로 여러 통계 정보를 요청했을 때 간헐적으로 요청이 실패하는 문제가 발생하는 상황이었습니다.

# 코드
### Entity
A와 B 두 엔티티가 있습니다. A와 B는 `OneToOne`관계이며 B가 연관관계의 주인으로 매핑되어 있습니다.


```java
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class A {
    @Id
    private Long id;

    @OneToOne(mappedBy = "a")
    private B b;

    private int var1;
}
```

```java
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class B {
    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "a_id")
    private A a;

    private int var1;
}
```
A와 B 둘 다 사용할 수 있는 Dto도 하나 존재합니다.
```java
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Dto {
    private Long id;
    private int var1;
}
```


### Repository
Repository에는 A와 B의 값을 하나 가져오는 findAFetchFirst와 findBFetchFirst가 존재합니다. 둘 모두 `Projections.constructor`를 이용해 Dto를 반환합니다.
```java
public interface Repository {
    Dto findAFetchFirst();
    Dto findBFetchFirst();
}
```
```java
@Repository
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

```
### Service
Repository의 메서드를 그대로 사용합니다.
```java
@Service
@RequiredArgsConstructor
public class Service {
    private final Repository repo;

    @Transactional
    public Dto getAData(){
        return repo.findAFetchFirst();
    }

    @Transactional
    public Dto getBData(){
        return repo.findBFetchFirst();
    }
}
```



### TestCode
[초기 데이터]
DB에는 다음과 같은 데이터가 들어있습니다.
```sql
insert into a values(1,1); // id, var1
insert into b values(1,99,1); // id, var1, a_id
```
쓰레드 A에서 A값을 가져오는 querydsl을, 쓰레드 B에서 B값을 가져오는 querydsl을 실행합니다.
```java
@SpringBootTest
@Slf4j
@Transactional
public class finalTest {
    @Autowired
    Service service;
    
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
}
```
*	데드락이 발생하면 쓰레드가 종료되지 않고 계속 실행됩니다. 이를 판단하기 위해 `Assertions.assertTimeoutPreemptively`를 사용했습니다. `assertTimeout`은 테스트가 완료될때까지 기다리지만 `assertTimeoutPreemptively`는 테스트 타임아웃이 지나면 즉시 테스트를 종료합니다. 
*	각 쓰레드가 시작은 했지만 결과값인 dto를 가지고나오지 못합니다.
	![](https://velog.velcdn.com/images/qwerty1434/post/3202b9eb-19e0-4cb1-93a7-d42ca9d8eb22/image.png)


### 기타
문제를 해결하기 전에 `A와 B를 같은 쓰레드에서 모두 실행`, `Hikari CP의 leak에 대한 테스트`, `A쓰레드와 B쓰레드 모두 A데이터를 호출함`, `A쓰레드와 B쓰레드 모두 B데이터를 호출함`, `A데이터와 B데이터를 쓰레드가 없이 같이 호출함`, `A데이터를 A쓰레드에서 호출하고 B쓰레드는 아무런 호출을 하지 않음`, `모든 작업을 querydsl로직이 아니라 Qtype호출에 대한 작업으로 변경해서 실행`등등 정말 많은 테스트를 진행했습니다. 테스트가 많았지만 문제를 이해하는데 방해가 될 거 같아 작성을 제외했습니다. 
대신 (당시에) 가장 의문스러웠던 테스트 하나만 소개드리겠습니다. RepositoryImpl의 findBFetchFirst()메서드를 아래 코드로 바꾸면 `데드락이 발생하지 않습니다.`
```java
    @Override
    public Dto findBFetchFirst(){
    	System.out.println(a.getClass()); // 이 부분만 새롭게 추가됐습니다.
        return queryFactory
                .select(Projections.constructor(Dto.class,
                        b.id,
                        b.var1
                ))
                .from(b)
                .fetchFirst();
    }
```
*	Q타입 B를 사용하기 전에 Q타입 A를 미리 호출해주면 데드락이 발생하지 않습니다.
*	결과
	![](https://velog.velcdn.com/images/qwerty1434/post/2f072e4e-b8d6-4d13-b6b7-9d0586c33a2a/image.png)



# 해결
querydsl공식문서에 다음과 같은 내용이 있습니다.
![](https://velog.velcdn.com/images/qwerty1434/post/0a092eaa-dec8-44a2-bff8-e49f4fd233de/image.png)
*	Q타입이 순환 의존을 가질 경우, 멀티 쓰레드 환경에서 Q타입을 초기화하면 데드락이 발생할 수 있다.
*	이에 대한 가장 쉬운 해결책은 멀티쓰레드 환경에서 사용되기 전에 단일 쓰레드에서 클래스를 초기화하는 것이다.
*	이런 목적으로 com.querydsl.codegen.ClassPathUtils 클래스를 사용할 수 있다.

OneToOne을 양방향 매핑하는 과정에서 A는 B를, B는 A를 참조하고 있었기 때문에 순환 의존을 가질 가능성이 충분하다고 판단했습니다. 공식문서의 내용처럼 단일 쓰레드에서 클래스를 초기화하기 위해 테스트코드를 다음과 같이 변경했습니다.
```java
    @Test
    void avoidDeadlock() throws IOException {
        ClassPathUtils.scanPackage(Thread.currentThread().getContextClassLoader(), 
        							"com.example.demo.domain");
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
```
*	`ClassPathUtils`를 사용하기 위해서는 의존성을 추가해줘야 합니다.
	build.gradle에 `implementation group: 'com.querydsl', name: 'querydsl-codegen', version: '5.0.0'` 추가
*	메인 쓰레드에서 엔티티 클래스를 초기화했기 때문에 데드락 문제가 발생하지 않습니다.
*	결과
	![](https://velog.velcdn.com/images/qwerty1434/post/a340b032-bd7a-460e-8216-1cabd72a88f5/image.png)

실제 프로젝트에서는 `@PostConstruct`시점에 클래스를 초기화 해 문제를 해결했습니다.
```java
@RestController
public class Controller {

    ...
    
    @PostConstruct
    public void init() throws Exception{
        ClassPathUtils.scanPackage(Thread.currentThread().getContextClassLoader(), 
        							"com.example.demo.domain");
    }
}
```

