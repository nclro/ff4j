package org.ff4j.parser.yaml;

/*-
 * #%L
 * ff4j-utils-yaml
 * %%
 * Copyright (C) 2013 - 2018 FF4J
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

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.ff4j.core.config.FF4jConfiguration;
import org.ff4j.core.config.FF4jConfigurationParser;
import org.ff4j.core.security.FF4jAcl;
import org.ff4j.core.security.FF4jGrantees;
import org.ff4j.core.security.FF4jPermission;
import org.ff4j.core.test.AssertUtils;
import org.ff4j.feature.Feature;
import org.ff4j.feature.ToggleStrategy;
import org.ff4j.property.Property;
import org.ff4j.property.PropertyString;
import org.ff4j.user.FF4jRole;
import org.ff4j.user.FF4jUser;
import org.yaml.snakeyaml.Yaml;

/**
 * Parser to read {@link FF4jConfiguration} from a YAML file.
 *
 * @author Cedrick LUNVEN (@clunven)
 */
public class YamlParser extends FF4jConfigurationParser {
    
    /**
     * Default constructor.
     */
    public YamlParser() {}
    
    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public FF4jConfiguration parse(InputStream inputStream) {
        AssertUtils.assertNotNull(inputStream, "Cannot read file stream is empty, check readability and path.");
        Yaml yaml = new Yaml();
        Map<?,?> yamlConfigFile = yaml.load(inputStream);
        Map<?,?> ff4jYamlMap = (Map<?, ?>) yamlConfigFile.get(FF4J_TAG);
        FF4jConfiguration ff4jConfig = new FF4jConfiguration();
        if (ff4jYamlMap != null) {
            // Audit
            if (ff4jYamlMap.containsKey(GLOBAL_AUDIT_TAG)) {
                ff4jConfig.setAudit(Boolean.valueOf(ff4jYamlMap.containsKey(GLOBAL_AUDIT_TAG)));
            }
            // AutoCreate
            if (ff4jYamlMap.containsKey(GLOBAL_AUTOCREATE)) {
                ff4jConfig.setAutoCreate(Boolean.valueOf(ff4jYamlMap.containsKey(GLOBAL_AUTOCREATE)));
            }
        
            // Roles
            parseRoles(ff4jConfig, (List<Map<String, Object>>) ff4jYamlMap.get(ROLES_TAG));
            
            // Users
            parseUsers(ff4jConfig, (List<Map<String, Object>>) ff4jYamlMap.get(USERS_TAG));
            
            // Properties
            ff4jConfig.getProperties()
                      .putAll(parseProperties((List<Map<String, Object>>) ff4jYamlMap.get(PROPERTIES_TAG))
            );
            
            // Features
            parseFeatures(ff4jConfig, (List<Map<String, Object>>) ff4jYamlMap.get(FEATURES_TAG));
        }
        return ff4jConfig;
    }
    
    /**
     * Read roles map and populate configuration bean.
     * 
     * @param ff4jConfig
     *      current configuration
     * @param rolesMap
     *      role map.
     */
    @SuppressWarnings("unchecked")
    private void parseRoles(FF4jConfiguration ff4jConfig, List<Map<String, Object>> roleItems) {
        if (null != roleItems) {
            roleItems.forEach(role -> {
                String roleName = (String) role.get(SECURITY_ROLE_ATTNAME);
                FF4jRole targetRole = new FF4jRole(roleName);
                ((Collection<String>) role.get(SECURITY_PERMISSIONS_TAG))
                        .stream()
                        .map(FF4jPermission::valueOf)
                        .forEach(targetRole::grant);
                ff4jConfig.getRoles().put(roleName, targetRole);
            }); 
        }
    }
    
    /**
     * Use 'users' tag to define users and their passwords.
     *
     * @param ff4jConfig
     *      configuration to populate
     * @param usersItems
     *      items loaded fron Yaml file.
     */
    @SuppressWarnings("unchecked")
    private void parseUsers(FF4jConfiguration ff4jConfig, List<Map<String, Object>> usersItems) {
        if (null != usersItems) {
            usersItems.forEach(user -> {
                FF4jUser currentUser = new FF4jUser( (String) user.get(USER_ATT_UID));
                currentUser.setDescription((String) user.get(USER_ATT_DESC));
                currentUser.setLastName((String) user.get(USER_ATT_LASTNAME));
                currentUser.setFirstName((String) user.get(USER_ATT_FIRSTNAME));
                if (user.containsKey(USER_ATT_ROLES)) {
                    currentUser.setRoles(new HashSet<String>((Collection<String>) user.get(USER_ATT_ROLES)));
                }
                if (user.containsKey(USER_ATT_PERMISSIONS)) {
                    currentUser.setPermissions(
                            ((Collection<String>) user.get(USER_ATT_PERMISSIONS))
                                .stream()
                                .map(FF4jPermission::valueOf)
                                .collect(Collectors.toSet()));
                }
                ff4jConfig.getUsers().put(currentUser.getUid(), currentUser);
            }); 
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<FF4jPermission, FF4jGrantees> parsePermission(Map<String, Object> yamlPermission) {
        List < String > users = (List<String>) yamlPermission.get(GLOBAL_PERMISSIONS_USERS);
        List < String > roles = (List<String>) yamlPermission.get(GLOBAL_PERMISSIONS_ROLES);
        Map<FF4jPermission, FF4jGrantees> value = new HashMap<>();
        value.put(
                FF4jPermission.valueOf((String) yamlPermission.get(GLOBAL_PERMISSIONS_NAME)), 
                new FF4jGrantees(users == null ? new HashSet<>() : new HashSet<>(users), 
                                 roles == null ? new HashSet<>() : new HashSet<>(roles)));
        return value;
    }
   
    @SuppressWarnings("unchecked")
    private Map < String, Property<?>> parseProperties(List<Map<String, Object>> properties) {
        Map < String, Property<?>> result = new HashMap<>();
        if (null != properties) {
            properties.forEach(property -> {
                // Initiate with name and value
                String name     = (String) property.get(PROPERTY_PARAMNAME);
                if (null == name) { 
                    throw new IllegalArgumentException("Invalid YAML File: 'name' is expected for properties");
                }
                
                Object objValue = property.get(PROPERTY_PARAMVALUE);
                if (null == objValue) {
                    throw new IllegalArgumentException("Invalid YAML File: 'value' is expected for properties");
                }
                // Convert as a String
                String strValue = String.valueOf(objValue);
                if (objValue instanceof Date) {
                    strValue = Property.SIMPLE_DATE_FORMAT.format((Date) objValue);
                }
                
                Property<?> ap = new PropertyString(name, strValue);
                String optionalType = (String) property.get(PROPERTY_PARAMTYPE);
                // If specific type defined ?
                if (null != optionalType) {
                    // Substitution if relevant (e.g. 'int' -> 'org.ff4j.property.PropertyInt')
                    Optional<String> mappedType = Property.mapFromSimple2PropertyType(optionalType);
                    if (mappedType.isPresent()) {
                        optionalType = mappedType.get();
                    }
                    
                    try {
                        // Constructor (String, String) is mandatory in Property interface
                        Constructor<?> constr = Class.forName(optionalType).getConstructor(String.class, String.class);
                        ap = (Property<?>) constr.newInstance(name, strValue);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Cannot instantiate '" + optionalType + "' check default constructor", e);
                    }
                }
                // Description
                String description = (String) property.get(PROPERTY_PARAMDESCRIPTION);
                if (null != description) {
                    ap.setDescription(description);
                } 
                // Fixed Values
                List<Object> fixedValues = (List<Object>) property.get(PROPERTY_PARAMFIXED_VALUES);
                if (null != fixedValues && fixedValues.size() > 0) {
                    fixedValues.stream().map(Object::toString).forEach(ap::add2FixedValueFromString);
                }
                // Check fixed value
                if (ap.getFixedValues().isPresent() &&  !ap.getFixedValues().get().contains(ap.getValue())) {
                    throw new IllegalArgumentException("Cannot create property <" + ap.getUid() + 
                            "> invalid value <" + ap.getValue() + 
                            "> expected one of " + ap.getFixedValues());
                }
                result.put(ap.getUid(), ap);
            });
        }
        return result;
    }
    
    /**
     * Parsing YAML TAGS.
     *
     * @param ff4jConfig
     *      current configuration
     * @param features
     *      feature to populate
     */
    @SuppressWarnings("unchecked")
    private void parseFeatures(FF4jConfiguration ff4jConfig, List<Map<String, Object>> features) {
        if (null != features) {
            features.forEach(feature -> {
                String name = (String) feature.get(FEATURE_ATT_UID);
                if (null == name) throw new IllegalArgumentException("Invalid YAML File: 'uid' is expected for feature");
                Feature f = new Feature(name);
                // Enabled
                Boolean enabled = (Boolean) feature.get(FEATURE_ATT_ENABLE);
                if (null != enabled) f.setEnable(enabled);
                // Description
                String description = (String) feature.get(FEATURE_ATT_DESC);
                if (null != description) f.setDescription(description);
                // Group
                String groupName = (String) feature.get(FEATURE_ATT_GROUP);
                if (null != groupName) f.setGroup(groupName);
                
                // Toggle Strategies
                List<Map<String, Object>> toggleStrategies = 
                        (List<Map<String, Object>>) feature.get(TOGGLE_STRATEGIES_TAG);
                if (toggleStrategies != null) {
                    toggleStrategies.stream()
                                    .map(map -> parseToggleStrategy(f,map)).forEach(f::addToggleStrategy);
                }
                
                // Security
                List<Map<String, Object>> customPermissons = 
                        (List<Map<String, Object>>) feature.get(SECURITY_PERMISSIONS_TAG);
                if (customPermissons != null) {
                    FF4jAcl customAcl = new FF4jAcl();
                    customPermissons.stream()
                                    .map(this::parsePermission)
                                    .forEach(map -> customAcl.getPermissions().putAll(map));
                    f.setAccessControlList(customAcl);
                }
                // Custom Properties
                List<Map<String, Object>> customProperties = 
                        (List<Map<String, Object>>) feature.get(PROPERTIES_TAG);
                if (customProperties != null) {
                    f.setProperties(parseProperties(customProperties));
                }
                
                ff4jConfig.getFeatures().put(f.getUid(), f);
            });
        }
    }
    
    @SuppressWarnings("unchecked")
    private ToggleStrategy parseToggleStrategy(Feature feature, Map<String, Object> toggleStrategy) {
        List<Map<String, Object>> strategyProperties = 
                (List<Map<String, Object>>) toggleStrategy.get(PROPERTIES_TAG);
        
        Set <Property<?>> expectedProperties = new HashSet<>();
        if (strategyProperties != null) {
            parseProperties(strategyProperties).values().stream().forEach(expectedProperties::add);
        }
        return ToggleStrategy.of(
                feature.getUid(), 
                (String) toggleStrategy.get(TOGGLE_STRATEGY_ATTCLASS), 
                expectedProperties);
    }
    
    /** {@inheritDoc} */
    @Override
    public String export(FF4jConfiguration ff4jConfig) {
        AssertUtils.assertNotNull(ff4jConfig);
        StringBuilder yamlFile =  new StringBuilder()
            .append(yamlKey("ff4j", 0, false))
            .append(yamlValue("autocreate", ff4jConfig.isAutoCreate(), 2, false))
            .append(yamlValue("audit", ff4jConfig.isAudit(), 2, false));
        
        // Roles
        yamlFile.append(yamlKey("roles", 2, false));
        for (Map.Entry<String, FF4jRole> role : ff4jConfig.getRoles().entrySet()) {
            yamlFile.append(yamlValue("name", role.getKey(), 4, true));
            yamlFile.append(yamlValue("permissions", role.getValue().getPermissions(), 6, false));
        }
        
        // Users
        if (null != ff4jConfig.getUsers() && !ff4jConfig.getUsers().isEmpty()) {
            yamlFile.append(yamlKey("users", 2, false));
            for (Map.Entry<String, FF4jUser> user : ff4jConfig.getUsers().entrySet()) {
                yamlFile.append(yamlValue("uid", user.getKey(), 4, true));
                yamlFile.append(yamlValue("firstname", user.getValue().getFirstName(), 6, false));
                yamlFile.append(yamlValue("lastname", user.getValue().getLastName(), 6, false));
                user.getValue().getDescription().ifPresent(desc -> 
                    yamlFile.append(yamlValue("description", desc, 6, false)));
                yamlFile.append(yamlValue("permissions", user.getValue().getPermissions(), 6, false));
                yamlFile.append(yamlValue("roles", user.getValue().getRoles(), 6, false));
            }
        }
        
        // Features
        if (null != ff4jConfig.getFeatures() && !ff4jConfig.getFeatures().isEmpty()) {
            yamlFile.append(yamlKey("features", 2, false));
            for (Feature f : ff4jConfig.getFeatures().values()) {
                yamlFile.append(yamlValue("uid", f.getUid(), 4, true));
                yamlFile.append(yamlValue("enable", f.isEnabled(), 6, false));
                f.getDescription().ifPresent(desc -> 
                yamlFile.append(yamlValue("description", desc, 6, false)));
                f.getGroup().ifPresent(g -> 
                yamlFile.append(yamlValue("groupName", g, 6, false)));
                if (!f.getProperties().isEmpty()) {
                    yamlFile.append(yamlKey("properties", 6, false));
                    if (!f.getProperties().isEmpty()) {
                        f.getProperties().values().stream().forEach(p-> {
                            yamlFile.append(yamlProperty(p,8));
                        });
                    }
                }
                if (!f.getToggleStrategies().isEmpty()) {
                    yamlFile.append(yamlKey("toggleStrategies", 6, false));
                    for (ToggleStrategy toggStrategy : f.getToggleStrategies()) {
                        yamlFile.append(yamlValue("class", toggStrategy.getClassName(), 8, true));
                        yamlFile.append(yamlKey("properties", 10, false));
                        toggStrategy.getProperties()
                                    .forEach(p-> { yamlFile.append(yamlProperty(p,12)); });
                    }
                }
                if (!f.getAccessControlList().getPermissions().isEmpty()) {
                    yamlFile.append(yamlKey("permissions", 6, false));
                    for (Map.Entry<FF4jPermission, FF4jGrantees> acl : f.getAccessControlList().getPermissions().entrySet()) {
                        yamlFile.append(yamlValue("name", acl.getKey(), 8, true));
                        if (null != acl.getValue().getRoles() && !acl.getValue().getRoles().isEmpty()) {
                            yamlFile.append(yamlValue("roles", acl.getValue().getRoles(), 10, false));
                        }
                        if (null != acl.getValue().getUsers() && !acl.getValue().getUsers().isEmpty()) {
                            yamlFile.append(yamlValue("users", acl.getValue().getUsers(), 10, false));
                        }
                    }
                }
            }
        }
        
        // Properties
        if (null != ff4jConfig.getProperties() && !ff4jConfig.getProperties().isEmpty()) {
            yamlFile.append(yamlKey("properties", 2, false));
            for (Property<?> prop : ff4jConfig.getProperties().values()) {
                yamlFile.append(yamlProperty(prop, 4));
            }
        }
        return yamlFile.toString();
    }
    
    private static final <T> String yamlProperty(Property<T> p, int offset) {
        StringBuilder yamlProp = new StringBuilder();
        yamlProp.append(yamlValue("name", p.getUid(), offset, true));
        yamlProp.append(yamlValue("type", p.getClass().getCanonicalName(), offset+2, false));
        yamlProp.append(yamlValue("value", p.getValueAsString(), offset+2, false));
        p.getFixedValues().ifPresent(fixedValues -> {
            yamlProp.append(yamlValue("fixedValues", fixedValues, offset+2, false));
        });
        return yamlProp.toString();
    }
    
    private static final String yamlKey(String key, int offset, boolean isList) {
        return  new StringBuilder()
                    .append(new String(new char[offset]).replace('\0', ' '))
                    .append(isList ? "- " : "")
                    .append(key).append(": ")
                    .append("\n").toString();
    }
    
    private static final String yamlValue(String key, Object value, int offset, boolean isList) {
       return new StringBuilder()
                   .append(new String(new char[offset]).replace('\0', ' '))
                   .append(isList ? "- " : "")
                   .append(key).append(": ")
                   .append(value)
                   .append("\n").toString();
    }
    
}
