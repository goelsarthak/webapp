package edu.neu.cloudwebapp.services;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import edu.neu.cloudwebapp.model.BillDetails;
import edu.neu.cloudwebapp.model.FileAttachment;
import edu.neu.cloudwebapp.repository.BillDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

@Service
@Profile("!local")
public class AWSFileHandlerService implements FileHandlerService {

    @Autowired
    private BillDetailsRepository billDetailsRepository;

    private AmazonS3 s3client;

    @Value("${amazon.s3.bucketName}")
    private String bucketName;

    @PostConstruct
    private void initializeAmazon() {
        this.s3client = AmazonS3ClientBuilder.standard().build();
    }

    @Override
    public FileAttachment uploadFile(MultipartFile attachment, BillDetails billDetails, String fileName) throws Exception {
        String name = billDetails.getId() + "/" + fileName;
        InputStream inputStream = null;
        ObjectMetadata metadata = new ObjectMetadata();
        try {
            inputStream = attachment.getInputStream();
            metadata.setContentLength(attachment.getSize());
            metadata.setContentType(attachment.getContentType());
        } catch (IOException e) {
            e.printStackTrace();
        }
        PutObjectResult result = s3client.putObject(bucketName, name, attachment.getInputStream(), metadata);
        S3Object obj = s3client.getObject(bucketName, name);


        FileAttachment fileAttachment = new FileAttachment();
        fileAttachment.setId(java.util.UUID.randomUUID().toString());
        fileAttachment.setFile_name(fileName);
        fileAttachment.setUrl(name);
        fileAttachment.setUpload_date(new Date());
        fileAttachment.setContentType(obj.getObjectMetadata().getContentType());
        fileAttachment.setHashcode(String.valueOf(obj.getObjectContent().hashCode()));
        fileAttachment.setFilesize(String.valueOf(obj.getObjectMetadata().getContentLength()));
        fileAttachment.setMd5Hash(result.getContentMd5());
        billDetails.setAttachment(fileAttachment);
        billDetailsRepository.save(billDetails);
        return fileAttachment;
    }

    @Override
    public boolean deleteFile(BillDetails billDetails) throws Exception {
        String fileName = billDetails.getAttachment().getUrl().toString();
        s3client.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(fileName));
        return true;
    }
}
