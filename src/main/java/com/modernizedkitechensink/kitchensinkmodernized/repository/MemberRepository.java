package com.modernizedkitechensink.kitchensinkmodernized.repository;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends MongoRepository<Member, String> {

  Optional<Member> findByEmail(String email);

  List<Member> findAllByOrderByNameAsc();

  /**
   * Search members by name, email, or phone number (case-insensitive).
   * Uses regex to match partial strings.
   * 
   * @param searchTerm The search term
   * @param pageable Pagination parameters
   * @return Page of matching members
   */
  @Query("{ $or: [ " +
         "{ 'name': { $regex: ?0, $options: 'i' } }, " +
         "{ 'email': { $regex: ?0, $options: 'i' } }, " +
         "{ 'phoneNumber': { $regex: ?0, $options: 'i' } } " +
         "] }")
  Page<Member> searchMembers(String searchTerm, Pageable pageable);
}