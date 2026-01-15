package com.modernizedkitechensink.kitchensinkmodernized.config;

import com.modernizedkitechensink.kitchensinkmodernized.model.Member;
import com.modernizedkitechensink.kitchensinkmodernized.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

  private final MemberRepository memberRepository;

  @Override
  public void run(String... args) {
    // Only seed if database is empty
    if (memberRepository.count() == 0) {
      Member johnSmith = new Member();
      johnSmith.setId("0");  // Same ID as legacy for compatibility
      johnSmith.setName("John Smith");
      johnSmith.setEmail("john.smith@mailinator.com");
      johnSmith.setPhoneNumber("2125551212");

      memberRepository.save(johnSmith);
      log.info("✓ Seeded default member: John Smith");
    }
  }
}
