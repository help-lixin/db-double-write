package help.lixin.data.sync.config;

import help.lixin.data.sync.DataSourceBeanPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSyncConfig {

    @Bean
    public BeanPostProcessor dataSourceBeanPostProcessor() {
        BeanPostProcessor beanPostProcessor = new DataSourceBeanPostProcessor();
        return beanPostProcessor;
    }
}
