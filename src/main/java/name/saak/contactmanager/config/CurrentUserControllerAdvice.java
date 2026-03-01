package name.saak.contactmanager.config;

import name.saak.contactmanager.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller Advice, der den aktuellen Benutzer in allen Views verfügbar macht.
 */
@ControllerAdvice
public class CurrentUserControllerAdvice {

    private final UserRepository userRepository;

    public CurrentUserControllerAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Fügt den aktuellen Benutzer zu allen Model-Objekten hinzu.
     */
    @ModelAttribute
    public void addCurrentUserToModel(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getPrincipal())) {

            String username = null;

            // OAuth2-Benutzer
            if (authentication.getPrincipal() instanceof CustomOAuth2User customUser) {
                username = customUser.getUser().getUsername();
            }
            // Lokale Benutzer
            else if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                username = userDetails.getUsername();
            }

            if (username != null) {
                userRepository.findByUsername(username).ifPresent(user -> {
                    model.addAttribute("currentUser", user);
                    model.addAttribute("currentUserDisplayName",
                        user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()
                            ? user.getDisplayName()
                            : user.getUsername());
                });
            }
        }
    }
}
