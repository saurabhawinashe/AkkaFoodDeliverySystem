from http import HTTPStatus
from threading import Thread
import requests
import time

# The sequence of two orders and agent sign in is changed
# One order is requested first
# Then concurrently the other order and one agent sign in done 
# Check if both the orders gets placed successfully
# but only one should be able to get agent


# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

	# Sign In One agent
	http_response = http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 201})

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

	# Place one new valid order 
	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 2, "qty": 4})
	
	if(http_response.status_code != HTTPStatus.CREATED):
		return 'Fail1'
	
	time.sleep(5)

	order_id1 = http_response.json().get("orderId")

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
	
	time.sleep(5)
	
	if(status_code1 == HTTPStatus.CREATED and status_code2 == HTTPStatus.CREATED):

		agent_id1 = result["1"].json().get("agentId")
		order_id2 = result["2"].json().get("orderId")
		
		http_response1 = requests.get(
		f"http://localhost:8081/order/{order_id1}")

		http_response2 = requests.get(
		f"http://localhost:8081/order/{order_id2}")
		
		if(http_response1.status_code != HTTPStatus.OK or http_response2.status_code != HTTPStatus.OK):
			return 'Fail2'

		res_body1 = http_response1.json()
		res_body2 = http_response2.json()	

		if(not((res_body1.get("agentId") == 201 and res_body2.get("agentId") == -1) or (res_body1.get("agentId") == -1 and res_body2.get("agentId") == 201))):
			return 'Fail3'

		if(not((res_body1.get("status") == "assigned" and res_body2.get("status") == "unassigned") or (res_body1.get("status") == "unassigned" and res_body2.get("status") == "assigned"))):
			return 'Fail4'
	
	else:
		return 'Fail'
		
	return 'Pass'

	
if __name__ == "__main__":
	print(test())
