package com.att.tdp.issueflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "issueflow.attachments")
@Getter
@Setter
public class AttachmentStorageProperties {

	private String uploadDir = "uploads";
}
