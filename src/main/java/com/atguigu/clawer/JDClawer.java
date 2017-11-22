package com.atguigu.clawer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.atguigu.clawer.pojo.Product;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JDClawer {
    
    private static final String base_url = "https://list.jd.com/list.html?cat=9987,653,655&page={page}";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    public void start() throws Exception{
        //程序入口
        String startUrl =StringUtils.replace(base_url, "{page}", "1");
        
        //获取网页内容
        String firstHtml = this.doget(startUrl);
        
        //解释html
        Document document = Jsoup.parse(firstHtml);
        String text = document.select("#J_topPage").text();
        //正则表达式  \\D+  整数
        String[] split = text.split("\\D+");
        
        Integer pageTotal = Integer.parseInt(split[1]);
        
        //思考:如何遍历所有页面...
        
        
        
        StringBuffer sb = new StringBuffer();
        
        for (int i = 1; i <= pageTotal; i++) {
            Map<Long, Product> maps = new HashMap<Long, Product>();
            String url =StringUtils.replace(base_url, "{page}", i+"");
            String html = doget(url);
            Document root = Jsoup.parse(html);
            Elements lis = root.select("#plist .gl-item");
            for (Element li : lis) {
                Product product = new Product();
                Element div = li.child(0);
                Long id = Long.parseLong(div.attr("data-sku"));
                String title = div.select(".p-name").text();
                String image= li.select(".p-img img").attr("src");
             
                product.setId(id);
                product.setTitle(title);
                product.setImage(image);
                
                maps.put(id, product);
            }
            List<String> ids = new ArrayList<String>();
            
            //获取商品价格  获取不到,因为价格是ajax异步请求.
            for (Long id: maps.keySet()) {
                ids.add("J_"+id);
            }
            String priceUrl ="https://p.3.cn/prices/mgets?skuIds="+StringUtils.join(ids, ",");
            String jsondata = this.doget(priceUrl);
            
            JsonNode jsonNode = MAPPER.readTree(jsondata);
            
            for (JsonNode jsonNode2 : jsonNode) {
                //J_123124124
                String strId = jsonNode2.get("id").textValue();
                Long id = Long.parseLong(StringUtils.substringAfter(strId, "_"));
                Long price = jsonNode2.get("p").asLong();
               
                //把价格存放在map集合中
                maps.get(id).setPrice(price);
                
            }
            //清空
            ids.clear();
            for (Long id: maps.keySet()) {
                ids.add("AD_"+id);
            }
            //获取广告
            String adUrl = "https://ad.3.cn/ads/mgets?skuids="+StringUtils.join(ids, ",");
            
            String jsondata2 = this.doget(adUrl);
            
            JsonNode jsonNode2 = MAPPER.readTree(jsondata2);
            
            for (JsonNode jsonNode3 : jsonNode2) {
                //J_123124124
                String strId = jsonNode3.get("id").textValue();
                Long id = Long.parseLong(StringUtils.substringAfter(strId, "_"));
                
                String sellpoint = jsonNode3.get("ad").asText();
                System.out.println(sellpoint);
               
                //把价格存放在map集合中
                maps.get(id).setSellpoint(sellpoint);
                
            }
            
            for (Product product2 : maps.values()) {
                sb.append(product2.toString());
                sb.append("\n");
            }
            
            FileUtils.writeStringToFile(new File("C:\\Users\\apple\\Desktop\\products.txt"), sb.toString());
            break;
        }
        
    }
    
    
    public String doget(String url) {
        try {
            // 创建Httpclient对象
            CloseableHttpClient httpclient = HttpClients.createDefault();

            // 创建http GET请求
            HttpGet httpGet = new HttpGet(url);

            CloseableHttpResponse response = null;
            try {
                // 执行请求
                response = httpclient.execute(httpGet);
                // 判断返回状态是否为200
                if (response.getStatusLine().getStatusCode() == 200) {
                    String content = EntityUtils.toString(response.getEntity(), "UTF-8");
                    return content;
                }
            } finally {
                if (response != null) {
                    response.close();
                }
                httpclient.close();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

}
