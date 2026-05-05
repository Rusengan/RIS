package com.coursework.driverservice.infrastructure.persistence.spec;

import com.coursework.driverservice.infrastructure.persistence.entity.RoleCode;
import com.coursework.driverservice.infrastructure.persistence.entity.RoleEntity;
import com.coursework.driverservice.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<UserEntity> hasRole(RoleCode role) {
        return (root, query, cb) -> {
            if (role == null) {
                return cb.conjunction();
            }
            query.distinct(true);
            Join<UserEntity, RoleEntity> rolesJoin = root.join("roles", JoinType.INNER);
            return cb.equal(rolesJoin.get("code"), role);
        };
    }

    public static Specification<UserEntity> isEnabled(Boolean enabled) {
        return (root, query, cb) -> enabled == null
                ? cb.conjunction()
                : cb.equal(root.get("enabled"), enabled);
    }
}
