package dio.budgeting.auth.infrastructure.security;

import dio.budgeting.auth.domain.UserAccount;
import dio.budgeting.auth.domain.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JpaUserDetailsService implements UserDetailsService {
    private final UserAccountRepository userAccountRepository;

    public JpaUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!StringUtils.hasText(username)) {
            throw new UsernameNotFoundException("Username must not be blank");
        }

        return userAccountRepository.findByUsername(UserAccount.normalizeUsername(username))
                .map(AuthenticatedUser::from)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
