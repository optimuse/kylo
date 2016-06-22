package com.thinkbiganalytics.metadata.modeshape.generic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.thinkbiganalytics.metadata.api.MetadataAccess;
import com.thinkbiganalytics.metadata.api.extension.ExtensibleEntity;
import com.thinkbiganalytics.metadata.api.extension.ExtensibleEntityProvider;
import com.thinkbiganalytics.metadata.api.extension.ExtensibleType;
import com.thinkbiganalytics.metadata.api.extension.ExtensibleTypeProvider;
import com.thinkbiganalytics.metadata.api.extension.FieldDescriptor;
import com.thinkbiganalytics.metadata.modeshape.ModeShapeEngineConfig;

@SpringApplicationConfiguration(classes = { ModeShapeEngineConfig.class, JcrExtensibleProvidersTestConfig.class })
public class JcrExtensibleProvidersTest extends AbstractTestNGSpringContextTests {

    @Inject
    private ExtensibleTypeProvider typeProvider;
    
    @Inject
    private ExtensibleEntityProvider entityProvider;
    
    @Inject
    private MetadataAccess metadata;
    

    @Test
    public void testGetAllDefaultTypes() {
        int size = metadata.commit(() -> {
            List<ExtensibleType> types = typeProvider.getTypes();
            
            return types.size();
        });
        
        // Feed + SLA + metric + FeedConnection + FeedSource + FeedDestination + Datasource = 7
        assertThat(size).isEqualTo(7);
    }

    @Test(dependsOnMethods="testGetAllDefaultTypes")
    public void testCreatePersonType() {
        String typeName = metadata.commit(() ->  {
            ExtensibleType type = typeProvider.buildType("Person")
                            .field("name")
                                .type(FieldDescriptor.Type.STRING)
                                .displayName("Person name")
                                .description("The name of the person")
                                .required(true)
                                .add()
                            .addField("description", FieldDescriptor.Type.STRING)
                            .addField("age", FieldDescriptor.Type.LONG)
                            .build();
                              
            return type.getName();
        });
        
        assertThat(typeName).isNotNull().isEqualTo("Person");
    }
    
    @Test(dependsOnMethods="testCreatePersonType")
    public void testCreateEmployeeType() {
        String typeName = metadata.commit(() -> {
            ExtensibleType person = typeProvider.getType("Person");
            ExtensibleType emp = typeProvider.buildType("Employee")
                            .supertype(person)
                            .field("name")
                                .type(FieldDescriptor.Type.STRING)
                                .displayName("Person name")
                                .description("The name of the person")
                                .required(true)
                                .add()
                            .addField("description", FieldDescriptor.Type.STRING)
                            .addField("age", FieldDescriptor.Type.LONG)
                            .build();
            
            return emp.getSupertype().getName();
        });
        
        assertThat(typeName).isNotNull().isEqualTo("Person");
    }
    
    @Test(dependsOnMethods="testCreatePersonType")
    public void testGetPersonType() {
        final ExtensibleType.ID id = metadata.commit(() -> {
            ExtensibleType type = typeProvider.getType("Person");
            
            return type.getId();
        });
        
        assertThat(id).isNotNull();
        
        Map<String, FieldDescriptor.Type> fields = metadata.commit(() -> {
            ExtensibleType type = typeProvider.getType("Person");
            Map<String, FieldDescriptor.Type> map = new HashMap<>();
            
            for (FieldDescriptor descr : type.getFieldDescriptors()) {
                map.put(descr.getName(), descr.getType());
            }
            
            return map;
        });
        
        assertThat(fields).isNotNull();
        assertThat(fields).containsEntry("name", FieldDescriptor.Type.STRING);
        assertThat(fields).containsEntry("description", FieldDescriptor.Type.STRING);
        assertThat(fields).containsEntry("age", FieldDescriptor.Type.LONG);
    }
    
    @Test(dependsOnMethods="testCreatePersonType")
    public void testGetAllTypes() {
        int size = metadata.commit(() -> {
            List<ExtensibleType> types = typeProvider.getTypes();
            
            return types.size();
        });
        
        // 7 + Person + Employee = 9
        assertThat(size).isEqualTo(9);
    }
    
    @Test(dependsOnMethods="testCreatePersonType")
    public void testCreateEntity() {
        ExtensibleEntity.ID id = metadata.commit(() -> {
            ExtensibleType type = typeProvider.getType("Person");
            
            Map<String, Object> props = new HashMap<>();
            props.put("name", "Bob");
            props.put("description", "Silly");
            props.put("age", 50);
            
            ExtensibleEntity entity = entityProvider.createEntity(type, props);
            
            return entity.getId();
        });
        
        assertThat(id).isNotNull();
    }
    
    @Test(dependsOnMethods="testCreatePersonType")
    public void testGetEntity() {
        String typeName = metadata.commit(() ->  {
            List<ExtensibleEntity> list = entityProvider.getEntities();
            
            assertThat(list).isNotNull().hasSize(1);
            
            ExtensibleEntity.ID id = list.get(0).getId();
            ExtensibleEntity entity = entityProvider.getEntity(id);
            
            assertThat(entity).isNotNull();
            assertThat(entity.getProperty("name")).isEqualTo("Bob");
            
            return entity.getTypeName();
        });
        
        assertThat(typeName).isEqualTo("Person");
    }
}