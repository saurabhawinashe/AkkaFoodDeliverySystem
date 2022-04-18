from http import HTTPStatus
from threading import Thread
import requests
import time

# Check if in the concurrent scenario, FulfillOrder actors are asigned to the proper agents.

# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def t1(result):  # First concurrent request

	# Sign In One agent
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 201})

	result["1"] = http_response


def t2(result):  # Second concurrent request

	# Customer 302 requests an order of item 1, quantity 3 from restaurant 101
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 202})

	result["2"] = http_response

def t3(result):  # First concurrent request

	# Sign In One agent
	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 301, "restId": 101, "itemId": 1, "qty": 2})

	result["3"] = http_response


def t4(result):  # Second concurrent request

	# Customer 302 requests an order of item 1, quantity 3 from restaurant 101
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 203})

	result["4"] = http_response

def t5(result, order_id1):  # Second concurrent request

	# Customer 302 requests an order of item 1, quantity 3 from restaurant 101
	http_response = requests.get(
		f"http://localhost:8081/order/{order_id1}")

	result["5"] = http_response

def t6(result, order_id2):  # Second concurrent request

	# Customer 302 requests an order of item 1, quantity 3 from restaurant 101
	http_response = http_response1 = requests.get(
		f"http://localhost:8081/order/{order_id2}")

	result["6"] = http_response

def t7(result, order_id3):  # Second concurrent request

	# Customer 302 requests an order of item 1, quantity 3 from restaurant 101
	http_response = http_response1 = requests.get(
		f"http://localhost:8081/order/{order_id3}")

	result["7"] = http_response


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

	order_id1 = http_response.json().get("orderId")

	http_response = requests.post(
		"http://localhost:8081/requestOrder", json={"custId": 302, "restId": 101, "itemId": 2, "qty": 4})
		
	if(http_response.status_code != HTTPStatus.CREATED):
		return 'Fail2'
	
	order_id2 = http_response.json().get("orderId")

	time.sleep(5)


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

		http_response1 = requests.get(
		f"http://localhost:8081/agent/201")

		http_response2 = requests.get(
		f"http://localhost:8081/agent/202")

		status1 = http_response1.json().get("status")
		status2 = http_response2.json().get("status")

		if(not(status1 == "unavailable" and status2 == "unavailable")):
			return 'Fail3'

		thread3 = Thread(target=t3, kwargs={"result": result})
		thread4 = Thread(target=t4, kwargs={"result": result})


		thread3.start()
		thread4.start()

		thread3.join()
		thread4.join()

		### Parallel Execution Ends ###
		status_code3 = result["3"].status_code
		status_code4 = result["4"].status_code	

		time.sleep(5)

		if(status_code3 != HTTPStatus.CREATED and status_code4!= HTTPStatus.CREATED):
			return 'Fail4'

		order_id3 = result["3"].json().get("orderId")

		thread5 = Thread(target=t5, kwargs={"result": result, "order_id1":order_id1})
		thread6 = Thread(target=t6, kwargs={"result": result, "order_id2":order_id2})
		thread7 = Thread(target=t7, kwargs={"result": result, "order_id3":order_id3})

		thread5.start()
		thread6.start()
		thread7.start()

		thread5.join()
		thread6.join()
		thread7.join()

		### Parallel Execution Ends ###
		status_code5 = result["5"].status_code
		status_code6 = result["6"].status_code	
		status_code7 = result["7"].status_code
		
		if(status_code5 != HTTPStatus.OK or status_code6 != HTTPStatus.OK or status_code7 != HTTPStatus.OK):
			return 'Fail5'

		time.sleep(10)

		agent_id1 = result["5"].json().get("agentId")
		agent_id2 = result["6"].json().get("agentId")	
		agent_id3 = result["7"].json().get("agentId")
		
		if(not(((agent_id1 == 201 and agent_id2 == 202) or (agent_id1 == 202 and agent_id2 == 201)) and agent_id3 == 203)):
			return 'Fail6'
	
	else:
		return 'Fail'
		
	return 'Pass'

	
if __name__ == "__main__":
	print(test())
