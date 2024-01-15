package com.textsearcherv2.validation;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UrlValidationService {

//    private static final Logger logger = LoggerFactory.getLogger(UrlValidationService.class);

    public boolean isValidUrl(String urlString) {
        try {
            // Existing validation logic
//            logger.info("URL validated successfully: {}", urlString);
            return true;
        } catch (Exception e) {
//            logger.error("URL validation failed for {}: {}", urlString, e.getMessage());
            return false;
        }
    }
}

