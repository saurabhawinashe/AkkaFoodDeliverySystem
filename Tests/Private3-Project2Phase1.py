from http import HTTPStatus
from threading import Thread
import requests

# Check if only one order is assigned when multiple concurrent requests
# for order comes, when only one delivery agent is available

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result, rest_id, item_id, qty):  # First concurrent request

    # Customer 301 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.post(
        "http://localhost:8080/refillItem", json={"restId":rest_id, "itemId":item_id, "qty":qty})

    result["1"] = http_response


def t2(result, rest_id, item_id, qty):  # Second concurrent request

    # Customer 302 requests an order of item 1, quantity 3 from restaurant 101
    http_response = requests.post(
        "http://localhost:8080/refillItem", json={"restId":rest_id, "itemId":item_id, "qty":qty})

    result["2"] = http_response


def test():

    result = {}

    # Reinitialize Restaurant service
    http_response = requests.post("http://localhost:8080/reInitialize")

    # Reinitialize Delivery service
    http_response = requests.post("http://localhost:8081/reInitialize")

    # Reinitialize Wallet service
    http_response = requests.post("http://localhost:8082/reInitialize")
        
    ### Parallel Execution Begins ###
    thread1 = Thread(target=t1, kwargs={"result": result, "rest_id":101, "item_id":1, "qty":10})
    thread2 = Thread(target=t2, kwargs={"result": result, "rest_id":101, "item_id":1, "qty":8})

    thread1.start()
    thread2.start()

    thread1.join()
    thread2.join()

    ### Parallel Execution Ends ###
    status_code1 = result["1"].status_code
    status_code2 = result["2"].status_code
    
    if(status_code1 != HTTPStatus.CREATED and status_code2 != HTTPStatus.CREATED):
        return "Fail90"

    # Check status of first order
    http_response = requests.post(
        f"http://localhost:8082/addBalance", json = {"custId":301, "amount":10000})
        
    if(http_response.status_code != HTTPStatus.CREATED):
        return "Fail11"
        
    http_response = requests.post(
        f"http://localhost:8081/requestOrder", json = {"custId":301, "restId":101, "itemId":1, "qty":28})

    if(http_response.status_code != HTTPStatus.CREATED):
        return 'Fail39'

    return 'Pass'


if __name__ == "__main__":

    print(test())
