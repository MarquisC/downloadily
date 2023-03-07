#!/bin/sh


BUCKET_NAME="test-bucket1"
awslocal s3api create-bucket --bucket ${BUCKET_NAME}

cat << EOT > bucket_policy.json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AdminBucketAccess",
            "Effect": "Allow",
            "Principal": "*",
            "Action": "s3:*",
            "Resource": "arn:aws:s3:::${BUCKET_NAME}/*"
        }
    ]
}
EOT


awslocal s3api put-bucket-policy --bucket ${BUCKET_NAME} --policy file://bucket_policy.json
