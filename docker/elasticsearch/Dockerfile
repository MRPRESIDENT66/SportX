# Elasticsearch 8.13.4 + IK 中文分词插件
# IK 让 "北京朝阳篮球馆" 这类中文文本能按词切分，支持 "篮球"/"朝阳" 等子词检索，
# 弥补 MySQL LIKE '%关键词%' 不分词、不走索引的短板。
# 版本与 Spring Boot 3.4.3 BOM 管理的 elasticsearch-java client (8.15.5) 对齐。
FROM docker.elastic.co/elasticsearch/elasticsearch:8.15.5

RUN bin/elasticsearch-plugin install --batch \
    https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.15.5.zip
