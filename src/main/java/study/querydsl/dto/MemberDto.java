package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 이게 없으면 기본생성자를 만들어 줘야함 public MemberDto() { }
public class MemberDto {

    private String username;
    private int age;

    @QueryProjection // 프로젝션과 결과 반환 (생성자 + @QueryProjection) -> 반드시 compileQuerydsl 해야함.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }

}
