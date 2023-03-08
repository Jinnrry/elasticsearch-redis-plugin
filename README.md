# redis数据源插件

> 目前支持的es版本为8.6.2



1、编译插件

```
mvn clean package
```

2、测试插件

```
cp target/redis-plugin-1.0.zip es-docker-compose/es/
cd es-docker-compose && docker-compose up --build -d
```

3、导入数据

```
PUT /demo
{
    "mappings": {
        "properties": {
            "product_id": {
                "type": "long"
            },
            "product_name": {
                "type": "text"
            }
        }
    }
}


PUT /demo/_doc/1
{
    "product_id":1,
    "product_name":"[国航]北京至三亚"
}

PUT /demo/_doc/2
{
    "product_id":2,
    "product_name":"[东航]北京至三亚"
}
```

4、执行查询

```
GET /demo/_search
{
  "query": {
    "bool": {
      "filter": {
        "script": {
          "script": {
            "source": "filter",
            "lang": "redis",
            "params": {
              "lt": "100",
              "gt": "300",
              "key_pre": "20230101_price_",
              "field": "product_id"
            }
          }
        }
      }
    }
  }
}
```

5、查询与排序

```
GET /demo/_search
{
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "filter": [
            {
              "script": {
                "script": {
                  "source": "filter",
                  "lang": "redis",
                  "params": {
                    "gt": "100",
                    "lt": "300",
                    "key_pre": "20230101_price_",
                    "field": "product_id"
                  }
                }
              }
            }
          ]
        }
      },
      "boost_mode": "replace",
      "functions": [
        {
          "script_score": {
            "script": {
              "source": "score",
              "lang": "redis",
              "params": {
                "field": "product_id",
                "key_pre": "20230101_price_"
              }
            }
          }
        }
      ]
    }
  }
}
```

向redis中插入数据，再测试
```
set 20230101_price_1 200
set 20230101_price_2 250
```


# Thanks:

https://www.jianshu.com/p/dca50c37fab7

https://github.com/BigDataBoutique/elasticsearch-rescore-redis`