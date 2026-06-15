# Elasticsearch 8.13.4 + IK 中文分词插件
# IK 让 "北京朝阳篮球馆" 这类中文文本能按词切分，支持 "篮球"/"朝阳" 等子词检索，
# 弥补 MySQL LIKE '%关键词%' 不分词、不走索引的短板。
FROM docker.elastic.co/elasticsearch/elasticsearch:8.13.4

RUN bin/elasticsearch-plugin install --batch \
    https://release.infinilabs.com/analysis-ik/stable/elasticsearch-analysis-ik-8.13.4.zip
