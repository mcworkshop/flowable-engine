/* Licensed under the Apache License, Version 2.0 (the "License");
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
 */
package org.flowable.test.spring.boot.idm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Filip Hrisafov
 */
public class IdmEngineAutoConfigurationTest {

    @Test
    public void standaloneIdmEngineWithBasicDataSource() {
        AnnotationConfigApplicationContext context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class,
            IdmEngineAutoConfiguration.class
        );

        IdmEngine idmEngine = context.getBean(IdmEngine.class);
        assertThat(idmEngine).as("Idm engine").isNotNull();
        assertAllServicesPresent(context, idmEngine);
    }

    @Test
    public void idmEngineWithBasicDataSourceAndProcessEngine() {
        AnnotationConfigApplicationContext context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            IdmEngineAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class
        );

        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        assertThat(processEngine).as("Process engine").isNotNull();
        IdmEngineConfiguration idmProcessConfiguration = idmEngine(processEngine);

        IdmEngine idmEngine = context.getBean(IdmEngine.class);
        assertThat(idmEngine).as("Idm engine").isNotNull();

        assertThat(idmEngine.getIdmEngineConfiguration()).as("Idm Engine Configuration").isEqualTo(idmProcessConfiguration);

        assertAllServicesPresent(context, idmEngine);
    }
    
    @Test
    public void idmEngineWithBasicDataSourceAndAppEngine() {
        AnnotationConfigApplicationContext context = this.context(
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            FlowableTransactionAutoConfiguration.class,
            AppEngineServicesAutoConfiguration.class,
            AppEngineAutoConfiguration.class,
            ProcessEngineServicesAutoConfiguration.class,
            ProcessEngineAutoConfiguration.class,
            IdmEngineAutoConfiguration.class,
            IdmEngineServicesAutoConfiguration.class
        );

        AppEngine appEngine = context.getBean(AppEngine.class);
        assertThat(appEngine).as("App engine").isNotNull();
        IdmEngineConfiguration idmProcessConfiguration = idmEngine(appEngine);

        IdmEngine idmEngine = context.getBean(IdmEngine.class);
        assertThat(idmEngine).as("Idm engine").isNotNull();

        assertThat(idmEngine.getIdmEngineConfiguration()).as("Idm Engine Configuration").isEqualTo(idmProcessConfiguration);

        assertAllServicesPresent(context, idmEngine);
        
        List<AppDeployment> appDeployments = appEngine.getAppRepositoryService().createDeploymentQuery().list();
        for (AppDeployment appDeployment : appDeployments) {
            appEngine.getAppRepositoryService().deleteDeployment(appDeployment.getId(), true);
        }
        
        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        List<Deployment> deployments = processEngine.getRepositoryService().createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

    private void assertAllServicesPresent(AnnotationConfigApplicationContext context, IdmEngine idmEngine) {
        List<Method> methods = Stream.of(IdmEngine.class.getDeclaredMethods())
            .filter(method -> !(method.getName().equals("close") || method.getName().equals("getName")))
            .collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType()))
                    .as(method.getReturnType() + " bean")
                    .isEqualTo(method.invoke(idmEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    private AnnotationConfigApplicationContext context(Class<?>... clazz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clazz);
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }

    private static IdmEngineConfiguration idmEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getIdmEngineConfiguration(processEngineConfiguration);
    }
    
    private static IdmEngineConfiguration idmEngine(AppEngine appEngine) {
        AppEngineConfiguration appEngineConfiguration = appEngine.getAppEngineConfiguration();
        return EngineServiceUtil.getIdmEngineConfiguration(appEngineConfiguration);
    }
}
