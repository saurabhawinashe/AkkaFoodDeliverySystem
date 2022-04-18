from http import HTTPStatus
from threading import Thread
import requests
import time

# Check if both the orders gets placed successfully and only one should be assigned to an agent

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

	# Customer 301 requests an order of item 1, quantity 3 from restaurant 101
	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 2, "qty": 4})

	result["1"] = http_response


def t2(result):  # Second concurrent request

	# Customer 302 requests an order of item 1, quantity 3 from restaurant 101
	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 2, "qty": 4})

	result["2"] = http_response


def test():

	result = {}

	# Reinitialize Restaurant service
	http_response = requests.post("http://localhost:8080/reInitialize")

	# Reinitialize Delivery service
	http_response = requests.post("http://localhost:8081/reInitialize")

	# Reinitialize Wallet service
	http_response = requests.post("http://localhost:8082/reInitialize")

	# Sign In One agent
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 201})
	
	time.sleep(2)

	if(http_response.status_code != HTTPStatus.CREATED):
		return 'Fail1'

	### Parallel Execution Begins ###
	thread1 = Thread(target=t1, kwargs={"result": result})
	thread2 = Thread(target=t2, kwargs={"result": result})

	thread1.start()
	thread2.start()

	thread1.join()
	thread2.join()

	### Parallel Execution Ends ###
	status_code1 = result["1"].status_code
	status_code2 = result["2"].status_code
	
	time.sleep(2)
	
	if(status_code1 == HTTPStatus.CREATED and status_code2 == HTTPStatus.CREATED):
		order_id1 = result["1"].json().get("orderId")
		order_id2 = result["2"].json().get("orderId")
		
		http_response1 = requests.get(
		f"http://localhost:8081/order/{order_id1}")
		
		if(http_response1.status_code != HTTPStatus.OK):
			return 'Fail2'

		res_body1 = http_response1.json()
			
		http_response2 = requests.get(
		f"http://localhost:8081/order/{order_id2}")
		
		if(http_response2.status_code != HTTPStatus.OK):
			return 'Fail4'
			
		res_body2 = http_response2.json()

		if(not((res_body1.get("agentId") == 201 and res_body2.get("agentId") == -1) or (res_body1.get("agentId") == -1 and res_body2.get("agentId") == 201))):
			print(res_body1.get("agentId"))
			print(res_body2.get("agentId"))		
			return 'Fail6'
	
	else:
		return 'Fail'
		
	return 'Pass'

	
if __name__ == "__main__":
	print(test())
