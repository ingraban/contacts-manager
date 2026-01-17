package name.saak.contactmanager.service;

import name.saak.contactmanager.domain.Role;
import name.saak.contactmanager.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Role> findRoleById(Long id) {
        return roleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Role> findRoleByName(String name) {
        return roleRepository.findByName(name);
    }
}
