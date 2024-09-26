package transaction.stage2;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

/**
 * 트랜잭션 전파(Transaction Propagation)란?
 * 트랜잭션의 경계에서 이미 진행 중인 트랜잭션이 있을 때 또는 없을 때 어떻게 동작할 것인가를 결정하는 방식을 말한다.
 *
 * FirstUserService 클래스의 메서드를 실행할 때 첫 번째 트랜잭션이 생성된다.
 * SecondUserService 클래스의 메서드를 실행할 때 두 번째 트랜잭션이 어떻게 되는지 관찰해보자.
 *
 * https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#tx-propagation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Stage2Test {

    private static final Logger log = LoggerFactory.getLogger(Stage2Test.class);

    @Autowired
    private FirstUserService firstUserService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    /**
     * 생성된 트랜잭션이 몇 개인가?
     * - 1개
     * 왜 그런 결과가 나왔을까?
     * - REQUIRED 옵션은 부모 트랜잭션이 존재한다면 합류, 존재하지 않는다면 새로 생성하기 때문
     */
    @Test
    void testRequired() {
        final var actual = firstUserService.saveFirstTransactionWithRequired();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.FirstUserService.saveFirstTransactionWithRequired");
    }

    /**
     * 생성된 트랜잭션이 몇 개인가?
     * - 2개
     * 왜 그런 결과가 나왔을까?
     * - REQUIRES NEW 옵션은 부모 트랜잭션의 여부와 관계없이 항상 새로운 트랜잭션을 생성하기 때문
     */
    @Test
    void testRequiredNew() {
        final var actual = firstUserService.saveFirstTransactionWithRequiresNew();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(2)
                .containsExactly("transaction.stage2.FirstUserService.saveFirstTransactionWithRequiresNew",
                        "transaction.stage2.SecondUserService.saveSecondTransactionWithRequiresNew");
    }

    /**
     * firstUserService.saveAndExceptionWithRequiredNew()에서 강제로 예외를 발생시킨다.
     * REQUIRES_NEW 일 때 예외로 인한 롤백이 발생하면서 어떤 상황이 발생하는 지 확인해보자.
     * - 자식 트랜잭션이 REQUIRES NEW로 되어 있고, 내부적으로 유저를 저장하고 있기 때문에
     * 부모 트랜잭션과 관계없이 정상적으로 커밋된다. 하지만, 부모 트랜잭션은 예외를 발생했기 때문에 정상적으로 수행되지 않는다.
     */
    @Test
    void testRequiredNewWithRollback() {
        assertThat(firstUserService.findAll()).hasSize(0);

        assertThatThrownBy(() -> firstUserService.saveAndExceptionWithRequiresNew())
                .isInstanceOf(RuntimeException.class);

        assertThat(firstUserService.findAll()).hasSize(1);
    }

    /**
     * FirstUserService.saveFirstTransactionWithSupports() 메서드를 보면 @Transactional이 주석으로 되어 있다.
     * 주석인 상태에서 테스트를 실행했을 때와 주석을 해제하고 테스트를 실행했을 때 어떤 차이점이 있는지 확인해보자.
     *
     * SUPPORTS는 부모 트랜잭션이 있다면 합류, 없다면 트랜잭션없이 실행되는 전파이다.
     *
     * 따라서 주석을 제거할 경우, 부모 트랜잭션만 존재하고 자식 트랜잭션은 존재하지 않게 된다.
     * 하지만 주석이 있는 경우, 부모 트랜잭션은 존재하지 않기 때문에 자식 트랜잭션만 존재하게 된다.
     */
    @Test
    void testSupports() {
        final var actual = firstUserService.saveFirstTransactionWithSupports();

        assertThat(firstUserService.findAll()).hasSize(2);

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithSupports");
    }

    /**
     * FirstUserService.saveFirstTransactionWithMandatory() 메서드를 보면 @Transactional이 주석으로 되어 있다.
     * 주석인 상태에서 테스트를 실행했을 때와 주석을 해제하고 테스트를 실행했을 때 어떤 차이점이 있는지 확인해보자.
     * SUPPORTS와 어떤 점이 다른지도 같이 챙겨보자.
     *
     * MANDATORY는 부모 트랜잭션이 존재한다면 합류하고, 존재하지 않는다면 예외를 발생한다.
     *
     * 주석을 제거할 경우, 부모 트랜잭션에 합류했기 때문에 부모 트랜잭션만 존재하게 된다.
     * 주석이 있는 경우, MANDATORY는 부모 트랜잭션이 무조건적으로 존재해야 하기 때문에 예외가 발생한다.
     */
    @Test
    void testMandatory() {
        final var actual = firstUserService.saveFirstTransactionWithMandatory();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.FirstUserService.saveFirstTransactionWithMandatory");
    }

    /**
     * 아래 테스트는 몇 개의 물리적 트랜잭션이 동작할까?
     * - 물리적 트랜잭션 1개, 논리적 트랜잭션 2개
     * FirstUserService.saveFirstTransactionWithNotSupported() 메서드의 @Transactional을 주석 처리하자.
     * 다시 테스트를 실행하면 몇 개의 물리적 트랜잭션이 동작할까?
     * - 물리적 트랜잭션 0개, 논리적 트랜잭션 1개
     * 스프링 공식 문서에서 물리적 트랜잭션과 논리적 트랜잭션의 차이점이 무엇인지 찾아보자.
     * - 트랜잭션 이름과 같은 속성은 논리적으로 트랜잭션이 존재함을 뜻한다. 다만, NOT SUPPORTED의 경우,
     * 실제로 트랜잭션이 존재하는 것이 아니기 때문에 (트랜잭션을 사용하지 않게 하기 때문) 물리적으로는
     * 존재하는 트랜잭션이 없는 것이다.
     */
    @Test
    void testNotSupported() {
        final var actual = firstUserService.saveFirstTransactionWithNotSupported();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(2)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNotSupported",
                        "transaction.stage2.FirstUserService.saveFirstTransactionWithNotSupported");
    }

    /**
     * 아래 테스트는 왜 실패할까?
     * FirstUserService.saveFirstTransactionWithNested() 메서드의 @Transactional을 주석 처리하면 어떻게 될까?
     *
     * 이미 존재하는 트랜잭션이 있는 경우, 해당 트랜잭션에 대한 세이브 포인트를 기록한다.
     * (다만, JpaDialect에서는 savepoint를 지원하지 않기 때문에 예외가 발생한다.)
     * 중첩된 트랜잭션은 먼저 시작된 부모 트랜잭션의 커밋과 롤백에는 영향을 받지만
     * 자신의 커밋과 롤백은 부모 트랜잭션에게 영향을 주지 않는다.
     * 메인 트랜잭션이 롤백되면 중첩된 로그 트랜잭션도 같이 롤백되지만,
     * 반대로 중첩된 로그 트랜잭션이 롤백돼도 메인 작업에 이상이 없다면 메인 트랜잭션은 정상적으로 커밋된다.
     */
    @Test
    void testNested() {
        final var actual = firstUserService.saveFirstTransactionWithNested();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNested");
    }

    /**
     * 마찬가지로 @Transactional을 주석처리하면서 관찰해보자.
     *
     * 트랜잭션을 사용하지 않도록 강제한다. 이미 진행 중인 트랜잭션도 존재하면 안된다 있다면 예외를 발생시킨다.
     */
    @Test
    void testNever() {
        final var actual = firstUserService.saveFirstTransactionWithNever();

        log.info("transactions : {}", actual);
        assertThat(actual)
                .hasSize(1)
                .containsExactly("transaction.stage2.SecondUserService.saveSecondTransactionWithNever");
    }
}
