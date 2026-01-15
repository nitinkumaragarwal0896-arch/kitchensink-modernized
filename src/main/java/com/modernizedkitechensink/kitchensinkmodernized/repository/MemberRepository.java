package com.modernizedkitechensink.kitchensinkmodernized.repository;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {

  Optional<Member> findByEmail(String email);

  List<Member> findAllByOrderByNameAsc();
}