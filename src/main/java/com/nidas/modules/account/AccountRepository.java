package com.nidas.modules.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, Long>, AccountRepositoryExtension {

    boolean existsByEmail(String email);

    Account findByEmail(String email);

}
