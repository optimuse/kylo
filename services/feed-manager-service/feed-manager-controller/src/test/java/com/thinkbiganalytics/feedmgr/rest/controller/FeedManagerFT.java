package com.thinkbiganalytics.feedmgr.rest.controller;

/*-
 * #%L
 * kylo-feed-manager-controller
 * %%
 * Copyright (C) 2017 ThinkBig Analytics
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.jayway.restassured.response.Response;
import com.thinkbiganalytics.discovery.model.DefaultDataTypeDescriptor;
import com.thinkbiganalytics.discovery.model.DefaultField;
import com.thinkbiganalytics.discovery.model.DefaultTableSchema;
import com.thinkbiganalytics.discovery.schema.Field;
import com.thinkbiganalytics.feedmgr.rest.model.FeedCategory;
import com.thinkbiganalytics.feedmgr.rest.model.FeedMetadata;
import com.thinkbiganalytics.feedmgr.rest.model.FeedSchedule;
import com.thinkbiganalytics.feedmgr.rest.model.FeedSummary;
import com.thinkbiganalytics.feedmgr.rest.model.ImportOptions;
import com.thinkbiganalytics.feedmgr.rest.model.NifiFeed;
import com.thinkbiganalytics.feedmgr.rest.model.RegisteredTemplate;
import com.thinkbiganalytics.feedmgr.rest.model.Tag;
import com.thinkbiganalytics.feedmgr.rest.model.schema.PartitionField;
import com.thinkbiganalytics.feedmgr.rest.model.schema.TableOptions;
import com.thinkbiganalytics.feedmgr.rest.model.schema.TableSetup;
import com.thinkbiganalytics.feedmgr.service.ExportImportTemplateService;
import com.thinkbiganalytics.feedmgr.service.feed.ExportImportFeedService;
import com.thinkbiganalytics.nifi.rest.model.NifiProperty;
import com.thinkbiganalytics.policy.rest.model.FieldPolicy;
import com.thinkbiganalytics.policy.rest.model.FieldStandardizationRule;
import com.thinkbiganalytics.policy.rest.model.FieldValidationRule;
import com.thinkbiganalytics.test.FunctionalTest;

import org.apache.nifi.web.api.dto.PortDTO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Functional Test for Feed Manager
 */
public class FeedManagerFT extends FunctionalTest {

    private static final String SAMPLES_DIR = "/samples";
    private static final String DATA_SAMPLES_DIR = SAMPLES_DIR + "/sample-data/";
    private static final String TEMPLATE_SAMPLES_DIR = SAMPLES_DIR + "/templates/nifi-1.0/";
    private static final String FEED_SAMPLES_DIR = SAMPLES_DIR + "/feeds/nifi-1.0/";
    private static final String VAR_DROPZONE = "/var/dropzone";
    private static final String USERDATA1_CSV = "userdata1.csv";
    private String feedsPath;
    private String templatesPath;
    private String usersDataPath;
    private FieldStandardizationRule toUpperCase = new FieldStandardizationRule();
    private FieldValidationRule email = new FieldValidationRule();

    @Before
    public void setup() throws URISyntaxException {
        String path = getClass().getResource(".").toURI().getPath();
        String basedir = path.substring(0, path.indexOf("services"));
        feedsPath = basedir + FEED_SAMPLES_DIR;
        templatesPath = basedir + TEMPLATE_SAMPLES_DIR;
        usersDataPath = basedir + DATA_SAMPLES_DIR;

        toUpperCase.setName("Uppercase");
        toUpperCase.setDisplayName("Uppercase");
        toUpperCase.setDescription("Convert string to uppercase");
        toUpperCase.setObjectClassType("com.thinkbiganalytics.policy.standardization.UppercaseStandardizer");
        toUpperCase.setObjectShortClassType("UppercaseStandardizer");

        email.setName("email");
        email.setDisplayName("Email");
        email.setDescription("Valid email address");
        email.setObjectClassType("com.thinkbiganalytics.policy.validation.EmailValidator");
        email.setObjectShortClassType("EmailValidator");
    }

    @Test
    public void testDataIngestFeed() {
        startClean();

        //create new category
        FeedCategory category = createCategory("Functional Tests");

        //import standard ingest template
        ExportImportTemplateService.ImportTemplate ingest = importTemplate("data_ingest.zip");
        Assert.assertEquals("data_ingest.zip",  ingest.getFileName());
        Assert.assertTrue(ingest.isSuccess());
        //assert new template is there
        RegisteredTemplate[] templates = getRegisteredTemplates();
        Assert.assertTrue(templates.length == 1);

        //create standard ingest feed
        FeedMetadata feed = createFeedRequest(category, ingest, "Users");
        FeedMetadata response = createFeed(feed);
        Assert.assertEquals(feed.getFeedName(), response.getFeedName());

        //drop files in dropzone to run the feed
        scp(usersDataPath + "/" + USERDATA1_CSV, VAR_DROPZONE);
        ssh(String.format("chown -R nifi:nifi %s", VAR_DROPZONE));
    }

    private void startClean() {
        deleteExistingReusableVersionedFlows();
        deleteExistingFeeds();
        deleteExistingTemplates();
        deleteExistingCategories();
        importSystemFeeds();
    }

    @Test
    public void importSystemFeeds() {
        importFeed("index_schema_service.zip");
        importFeed("index_text_service.zip");
    }

    public void deleteExistingReusableVersionedFlows() {
        //otherwise if we don't delete each time we import a new template
        // exiting templates are versioned off and keep piling up
        PortDTO[] ports = getReusableInputPorts();
        for (PortDTO port : ports) {
            deleteVersionedNifiFlow(port.getParentGroupId());
        }
    }

    private void deleteVersionedNifiFlow(String groupId) {
        Response response = given(NifiIntegrationRestController.BASE)
            .when()
            .get("/cleanup-versions/" + groupId);

        response.then().log().all().statusCode(200);
    }

    private PortDTO[] getReusableInputPorts() {
        Response response = given(NifiIntegrationRestController.BASE)
            .when()
            .get(NifiIntegrationRestController.REUSABLE_INPUT_PORTS);

        response.then().log().all().statusCode(200);

        return response.as(PortDTO[].class);
    }

    private void deleteExistingCategories() {
        //start clean - delete all categories if there
        FeedCategory[] categories = getCategories();
        for (FeedCategory category : categories) {
            deleteCategory(category.getId());
        }
        categories = getCategories();
        Assert.assertTrue(categories.length == 0);
    }

    private void deleteExistingTemplates() {
        //start clean - delete all templates if there
        RegisteredTemplate[] templates = getRegisteredTemplates();
        for (RegisteredTemplate template : templates) {
            deleteTemplate(template.getId());
        }
        //assert there are no templates
        templates = getRegisteredTemplates();
        Assert.assertTrue(templates.length == 0);
    }

    private void deleteExistingFeeds() {
        //start clean - delete all feeds
        FeedSummary[] feeds = getFeeds();
        for (FeedSummary feed : feeds) {
            deleteFeed(feed.getFeedId());
        }
        feeds = getFeeds();
        Assert.assertTrue(feeds.length == 0);
    }

    private FeedMetadata createFeedRequest(FeedCategory category, ExportImportTemplateService.ImportTemplate template, String name) {
        FeedMetadata feed = new FeedMetadata();
        feed.setFeedName(name);
        feed.setSystemFeedName(name.toLowerCase());
        feed.setCategory(category);
        feed.setTemplateId(template.getTemplateId());
        feed.setTemplateName(template.getTemplateName());
        feed.setDescription("Created by functional test");
        feed.setInputProcessorType("org.apache.nifi.processors.standard.GetFile");

        List<NifiProperty> properties = new ArrayList<>();
        NifiProperty fileFilter = new NifiProperty("305363d8-015a-1000-0000-000000000000", "1f67e296-2ff8-4b5d-0000-000000000000", "File Filter", USERDATA1_CSV);
        fileFilter.setProcessGroupName("NiFi Flow");
        fileFilter.setProcessorName("Filesystem");
        fileFilter.setTemplateValue("mydata\\d{1,3}.csv");
        fileFilter.setInputProperty(true);
        fileFilter.setUserEditable(true);
        properties.add(fileFilter);
        NifiProperty inputDir = new NifiProperty("305363d8-015a-1000-0000-000000000000", "1f67e296-2ff8-4b5d-0000-000000000000", "Input Directory", VAR_DROPZONE);
        inputDir.setProcessGroupName("NiFi Flow");
        inputDir.setProcessorName("Filesystem");
        inputDir.setInputProperty(true);
        inputDir.setUserEditable(true);
        properties.add(inputDir);
        NifiProperty loadStrategy = new NifiProperty("305363d8-015a-1000-0000-000000000000", "6aeabec7-ec36-4ed5-0000-000000000000", "Load Strategy", "FULL_LOAD");
        properties.add(loadStrategy);
        feed.setProperties(properties);

        FeedSchedule schedule = new FeedSchedule();
        schedule.setConcurrentTasks(1);
        schedule.setSchedulingPeriod("15 sec");
        schedule.setSchedulingStrategy("TIMER_DRIVEN");
        feed.setSchedule(schedule);

        TableSetup table = new TableSetup();
        DefaultTableSchema schema = new DefaultTableSchema();
        schema.setName("test1");
        List<Field> fields = new ArrayList<>();
        fields.add(newTimestampField("registration_dttm"));
        fields.add(newBigIntField("id"));
        fields.add(newStringField("first_name"));
        fields.add(newStringField("last_name"));
        fields.add(newStringField("email"));
        fields.add(newStringField("gender"));
        fields.add(newStringField("ip_address"));
        fields.add(newStringField("cc"));
        fields.add(newStringField("country"));
        fields.add(newStringField("birthdate"));
        fields.add(newStringField("salary"));
        fields.add(newStringField("title"));
        fields.add(newStringField("comments"));
        schema.setFields(fields);

        table.setTableSchema(schema);
        table.setSourceTableSchema(schema);
        table.setFeedTableSchema(schema);
        table.setTargetMergeStrategy("DEDUPE_AND_MERGE");
        table.setFeedFormat("ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'\n WITH SERDEPROPERTIES ( 'separatorChar' = ',' ,'escapeChar' = '\\\\' ,'quoteChar' = '\\'') STORED AS TEXTFILE");
        table.setTargetFormat("STORED AS ORC");

        List<FieldPolicy> policies = new ArrayList<>();
        policies.add(newEmptyPolicy("registration_dttm"));
        policies.add(newEmptyPolicy("id"));
        policies.add(newPolicyWithProfileAndIndex("first_name", toUpperCase));
        policies.add(newPolicyWithProfileAndIndex("last_name"));
        policies.add(newPolicyWithValidation("email", email));
        policies.add(newEmptyPolicy("ip_address"));
        policies.add(newEmptyPolicy("cc"));
        policies.add(newEmptyPolicy("country"));
        policies.add(newEmptyPolicy("birthdate"));
        policies.add(newEmptyPolicy("salary"));
        policies.add(newEmptyPolicy("title"));
        policies.add(newEmptyPolicy("comments"));
        table.setFieldPolicies(policies);

        List<PartitionField> partitions = new ArrayList<>();
        partitions.add(byYear("registration_dttm"));
        table.setPartitions(partitions);

        TableOptions options = new TableOptions();
        options.setCompressionFormat("SNAPPY");
        options.setAuditLogging(true);
        table.setOptions(options);

        table.setTableType("SNAPSHOT");
        feed.setTable(table);

        feed.setDataOwner("Marketing");

        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("users"));
        tags.add(new Tag("registrations"));
        feed.setTags(tags);

        return feed;
    }

    private FeedMetadata createFeed(FeedMetadata feed) {
        Response response = given(FeedRestController.BASE)
            .body(feed)
            .when()
            .post();

        response.then().log().all().statusCode(200);

        NifiFeed nifiFeed = response.as(NifiFeed.class);
        return nifiFeed.getFeedMetadata();
    }

    private PartitionField byYear(String fieldName) {
        PartitionField part = new PartitionField();
        part.setSourceField(fieldName);
        part.setField(fieldName + "_year");
        part.setFormula("year");
        part.setSourceDataType("timestamp");
        return part;
    }

    private FieldPolicy newPolicyWithValidation(String fieldName, FieldValidationRule... rules) {
        FieldPolicy policy = newEmptyPolicy(fieldName);
        List<FieldValidationRule> validationRules = new ArrayList<>();
        if (rules != null) {
            Collections.addAll(validationRules, rules);
        }
        policy.setValidation(validationRules);
        return policy;
    }

    private FieldPolicy newPolicyWithProfileAndIndex(String fieldName, FieldStandardizationRule... rules) {
        FieldPolicy policy = newEmptyPolicy(fieldName);
        policy.setProfile(true);
        policy.setIndex(true);
        List<FieldStandardizationRule> standardisation = new ArrayList<>();
        if (rules != null) {
            Collections.addAll(standardisation, rules);
        }
        policy.setStandardization(standardisation);
        return policy;
    }

    private FieldPolicy newEmptyPolicy(String fieldName) {
        FieldPolicy policy = new FieldPolicy();
        policy.setFieldName(fieldName);
        policy.setFeedFieldName(fieldName);
        return policy;
    }

    private DefaultField newStringField(String name) {
        return newNamedField(name, new DefaultDataTypeDescriptor(), "string");
    }

    private DefaultField newTimestampField(String name) {
        return newNamedField(name, new DefaultDataTypeDescriptor(), "timestamp");
    }

    private DefaultField newBigIntField(String name) {
        DefaultDataTypeDescriptor numericDescriptor = new DefaultDataTypeDescriptor();
        numericDescriptor.setNumeric(true);

        return newNamedField(name, numericDescriptor, "bigint");
    }

    private DefaultField newNamedField(String name, DefaultDataTypeDescriptor numericDescriptor, String bigint) {
        DefaultField field = new DefaultField();
        field.setName(name);
        field.setDerivedDataType(bigint);
        field.setDataTypeDescriptor(numericDescriptor);
        return field;
    }


    private FeedCategory[] getCategories() {
        Response response = given(FeedCategoryRestController.BASE)
            .when()
            .get();

        response.then().log().all().statusCode(200);

        return response.as(FeedCategory[].class);
    }

    private void deleteCategory(String id) {
        String url = String.format("/%s", id);
        Response response = given(FeedCategoryRestController.BASE)
            .when()
            .delete(url);

        response.then().statusCode(200);
    }

    private FeedCategory createCategory(String name) {
        FeedCategory category = new FeedCategory();
        category.setName(name);
        category.setDescription("this category was created by functional test");
        category.setIcon("account_balance");
        category.setIconColor("#FF8A65");

        Response response = given(FeedCategoryRestController.BASE)
            .body(category)
            .when()
            .post();

        response.then().log().all().statusCode(200);

        return response.as(FeedCategory.class);
    }


    private ExportImportFeedService.ImportFeed importFeed(String feedName) {
        Response post = given(AdminController.BASE)
            .contentType("multipart/form-data")
            .multiPart(new File(feedsPath + feedName))
            .multiPart("overwrite", true)
            .multiPart("importConnectingReusableFlow", ImportOptions.IMPORT_CONNECTING_FLOW.YES)
            .when().post(AdminController.IMPORT_FEED);

        post.then().statusCode(200);

        return post.as(ExportImportFeedService.ImportFeed.class);
    }

    private ExportImportTemplateService.ImportTemplate importTemplate(String templateName) {
        Response post = given(AdminController.BASE)
            .contentType("multipart/form-data")
            .multiPart(new File(templatesPath + templateName))
            .multiPart("overwrite", true)
            .multiPart("createReusableFlow", false)
            .multiPart("importConnectingReusableFlow", ImportOptions.IMPORT_CONNECTING_FLOW.YES)
            .when().post(AdminController.IMPORT_TEMPLATE);

        post.then().statusCode(200);

        return post.as(ExportImportTemplateService.ImportTemplate.class);
    }

    private FeedSummary[] getFeeds() {
        Response response = given(FeedRestController.BASE)
            .when()
            .get();

        response.then().log().all().statusCode(200);

        return response.as(FeedSummary[].class);
    }

    private void deleteFeed(String feedId) {
        String url = String.format("/%s", feedId);
        Response response = given(FeedRestController.BASE)
            .when()
            .delete(url);

//        if (response.statusCode() == 409) {
//            RestResponseStatus responseStatus = response.body().as(RestResponseStatus.class);
            //todo find id of referring feed and delete it if failed here because the feed is referenced by other feed
//        } else {
            response.then().statusCode(204);
//        }
    }

    private RegisteredTemplate[] getRegisteredTemplates() {
        Response response = given(TemplatesRestController.BASE)
            .when().get(TemplatesRestController.REGISTERED);

        response.then().log().all().statusCode(200);

        return response.as(RegisteredTemplate[].class);
    }

    private void deleteTemplate(String templateId) {
        String url = String.format("/registered/%s/delete", templateId);
        Response response = given(TemplatesRestController.BASE)
            .when()
            .delete(url);

        response.then().statusCode(200);
    }
}
