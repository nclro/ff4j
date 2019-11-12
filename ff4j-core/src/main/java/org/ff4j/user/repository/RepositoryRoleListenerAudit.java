package org.ff4j.user.repository;

/*-
 * #%L
 * ff4j-core
 * %%
 * Copyright (C) 2013 - 2019 FF4J
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.ff4j.core.FF4jEntity;
import org.ff4j.core.FF4jRepositoryListener;
import org.ff4j.event.Event;
import org.ff4j.event.Event.Action;
import org.ff4j.event.Event.Scope;
import org.ff4j.event.repository.audit.AuditTrailListenerSupport;
import org.ff4j.event.repository.audit.AuditTrailRepository;
import org.ff4j.user.FF4jRole;

/**
 * Public Interface of a Listener on CRUD repository.
 * @Do not put any onRead() as making not sense in ff4J.
 * 
 * @author Cedrick LUNVEN  (@clunven)
 *
 * @param <ENTITY>
 *    {@link FF4jEntity} to be specialized by type of store 
 */
public class RepositoryRoleListenerAudit extends AuditTrailListenerSupport < FF4jRole > implements FF4jRepositoryListener< FF4jRole > {

    /**
     * Default implementation of User Listener.
     *
     * @param auditTrail
     *      target audit trail to fill
     */
    public RepositoryRoleListenerAudit(AuditTrailRepository auditTrail) {
        super(auditTrail, Scope.USER, Scope.USERSTORE);
    }
    
    /** {@inheritDoc} */
    @Override
    public void onCreate(FF4jRole role) {
        log(Event.builder()
                .action(Action.CREATE)
                .scope(Scope.ROLE)
                .refEntityUid(role.getUid())
                .build());
    }
    
}
