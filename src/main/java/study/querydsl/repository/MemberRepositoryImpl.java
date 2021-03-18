package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    //회원명, 팀명, 나이(ageGoe, ageLoe)
    public List<MemberTeamDto> search(MemberSearchCondition condition) {
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return isEmpty(username) ? null : member.username.eq(username);
    }

    private BooleanExpression teamNameEq(String teamName) {
        return isEmpty(teamName) ? null : team.name.eq(teamName);
    }
    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }
    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }

    /**
     *  복잡한 페이징
     * 데이터 조회 쿼리와, 전체 카운트 쿼리를 분리
     * 전체 카운트를 조회 하는 방법을 최적화 할 수 있으면 이렇게 분리하면 된다.
     * (예를 들어서 전체 카운트를 조회할 때 조인 쿼리를 줄일 수 있다면 상당한 효과가 있다.)
     * 코드를 리펙토링해서 내용 쿼리과 전체 카운트 쿼리를 읽기 좋게 분리하면 좋다.
     */
    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = getMemberTeamDtoQueryResults(condition, pageable);

        // 데이터 반환
        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);

    }

    /**
     * 스프링 데이터 페이징 활용2 - CountQuery 최적화
     * PageableExecutionUtils.getPage()로 최적화
     */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        // 직접 토탈 카운트 쿼리를 날림
        List<MemberTeamDto> content = queryFactory
                        .select(new QMemberTeamDto(
                                member.id.as("memberId"),
                                member.username,
                                member.age,
                                team.id.as("teamId"),
                                team.name.as("teamName")))
                        .from(member)
                        .leftJoin(member.team, team)
                        .where(usernameEq(condition.getUsername()),
                                teamNameEq(condition.getTeamName()),
                                ageGoe(condition.getAgeGoe()),
                                ageLoe(condition.getAgeLoe()))
                        .offset(pageable.getOffset())       // 몇번부터 시작할건지
                        .limit(pageable.getPageSize())      // 몇개를 가져올건지
                        .fetch();

        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

        // () -> countQuery.fetchCount() 에서 countQuery::fetchCount 변경
        // 카운트쿼리가 필요하면 호출리고 안필요하면 호출하지 않음.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchCount);
    }

    private QueryResults<MemberTeamDto> getMemberTeamDtoQueryResults(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .offset(pageable.getOffset())       // 몇번부터 시작할건지
                .limit(pageable.getPageSize())      // 몇개를 가져올건지
                .fetchResults(); // queryDsl에 컨텐츠와 카운트 두개를 조회
        return results;
    }

    /**
     * 리포지토리 지원 - extends QuerydslRepositorySupport
     * QuerydslRepositorySupport을 상속받아서 사용
     * 장점 :
     * 1. getQuerydsl().applyPagination() 스프링 데이터가 제공하는 페이징을 Querydsl로 편리하게 변환 가능(단! Sort는 오류발생)
     * 2. from() 으로 시작 가능(최근에는 QueryFactory를 사용해서 select() 로 시작하는 것이 더 명시적)
     * 3. EntityManager 제공
     * 4. offset, limit 지원
     *
     * 한계(단점) :
     * 1. Querydsl 3.x 버전을 대상으로 만듬
     * 2. Querydsl 4.x에 나온 JPAQueryFactory로 시작할 수 없음
     * 3. select로 시작할 수 없음 (from으로 시작해야함)
     * 4. QueryFactory 를 제공하지 않음
     * 5. 스프링 데이터 Sort 기능이 정상 동작하지 않음 -> 큐 소트를 사용하면 되긴함. (소팅을 직접해줘야 된다는 의미)
     * 6. 메서드 체인이 끊김
     */

//    public MemberRepositoryImpl() {
//        super(Member.class);
//    }

    // QuerydslRepositorySupport 사용
    // from절이 먼저 시작합니다.
//    private Page<MemberTeamDto> getMemberTeamDtoQueryResults(MemberSearchCondition condition, Pageable pageable){
//        JPQLQuery<MemberTeamDto> jpaQuery = from(member)
//                .leftJoin(member.team, team)
//                .where(usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe()))
//                .select(new QMemberTeamDto(
//                        member.id.as("memberId"),
//                        member.username,
//                        member.age,
//                        team.id.as("teamId"),
//                        team.name.as("teamName")));
//
//        JPQLQuery<MemberTeamDto> query = getQuerydsl().applyPagination(pageable, jpaQuery);// offset, limit 지원
//
//        query.fetch();
//    }

    // QuerydslRepositorySupport을 사용한 방법
    // from절이 먼저 시작합니다.
//    @Override
//    public List<MemberTeamDto> search(MemberSearchCondition condition) {
//        List<MemberTeamDto> result = from(member)
//                .leftJoin(member.team, team)
//                .where(usernameEq(condition.getUsername()),
//                        teamNameEq(condition.getTeamName()),
//                        ageGoe(condition.getAgeGoe()),
//                        ageLoe(condition.getAgeLoe()))
//                .select(new QMemberTeamDto(
//                        member.id.as("memberId"),
//                        member.username,
//                        member.age,
//                        team.id.as("teamId"),
//                        team.name.as("teamName")))
//                .fetch();
//    }


}
