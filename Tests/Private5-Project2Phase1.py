from http import HTTPStatus
from threading import Thread
import requests

# Check if only one order is assigned when multiple concurrent requests
# for order comes

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

    # Customer 301 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 2, "qty": 8})

    result["1"] = http_response


def t2(result):  # Second concurrent request

    # Customer 302 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.post(
        "http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 2, "qty": 8})

    result["2"] = http_response


def test():

    result = {}
    testresult = "Pass"

    http_response = requests.post("http://localhost:8080/reInitialize")
    
    http_response = requests.post("http://localhost:8081/reInitialize")

    http_response = requests.post("http://localhost:8082/reInitialize")

    thread1 = Thread(target=t1, kwargs={"result": result})
    thread2 = Thread(target=t2, kwargs={"result": result})

    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()

    ### Parallel Execution Ends ###
    status_code1 = result["1"].status_code
    status_code2 = result["2"].status_code
    
    if(status_code1 != HTTPStatus.CREATED or status_code2 != HTTPStatus.CREATED):
        testresult = "Fail1"
        
    orderId1 = result["1"].json().get("orderId")
    orderId2 = result["2"].json().get("orderId")
    

    # Check status of first order
    http_response1 = requests.get(
        f"http://localhost:8081/order/{orderId1}")

    http_response2 = requests.get(
        f"http://localhost:8081/order/{orderId2}")

    if(http_response1.status_code != HTTPStatus.OK or http_response1.status_code != HTTPStatus.OK):
        testresult = "Fail2"

    res_body_1 = http_response1.json()
    res_body_2 = http_response2.json()

    order_status_1 = res_body_1.get("status")
    order_status_2 = res_body_2.get("status")

    if (order_status_1 != 'delivered' and order_status_2 != "delivered") or (order_status_1 != 'unassigned' and order_status_2 != "unassigned") :
       testresult =  "Fail3"

    return testresult


if __name__ == "__main__":

    print(test())
