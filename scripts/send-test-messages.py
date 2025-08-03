#!/usr/bin/env python3
"""
Script to send test order messages to Kafka
"""

import json
import time
import random
from datetime import datetime
from kafka import KafkaProducer
from kafka.errors import KafkaError

# Configuration
KAFKA_BOOTSTRAP_SERVERS = ['localhost:9092']
TOPIC_NAME = 'orders'

# Sample data
CUSTOMER_IDS = [
    'customer-001', 'customer-002', 'customer-003', 'customer-004', 'customer-005'
]

PRODUCT_IDS = [
    'product-001', 'product-002', 'product-003', 'product-004', 'product-005'
]

def create_order_message():
    """Create a random order message"""
    order_id = f"order-{int(time.time() * 1000)}"
    customer_id = random.choice(CUSTOMER_IDS)
    
    # Random number of products (1-3)
    num_products = random.randint(1, 3)
    product_ids = random.sample(PRODUCT_IDS, num_products)
    
    message = {
        "orderId": order_id,
        "customerId": customer_id,
        "productIds": product_ids,
        "timestamp": datetime.now().isoformat()
    }
    
    return order_id, message

def send_messages(num_messages=10, delay_seconds=2):
    """Send test messages to Kafka"""
    
    # Create Kafka producer
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode('utf-8'),
        key_serializer=lambda k: k.encode('utf-8') if k else None
    )
    
    print(f"🚀 Sending {num_messages} test messages to Kafka...")
    print(f"📊 Topic: {TOPIC_NAME}")
    print(f"⏱️  Delay between messages: {delay_seconds} seconds")
    print("-" * 50)
    
    for i in range(num_messages):
        order_id, message = create_order_message()
        
        # Send message
        future = producer.send(
            topic=TOPIC_NAME,
            key=order_id,
            value=message
        )
        
        try:
            record_metadata = future.get(timeout=10)
            print(f"✅ Message {i+1}/{num_messages} sent successfully:")
            print(f"   Order ID: {order_id}")
            print(f"   Customer: {message['customerId']}")
            print(f"   Products: {message['productIds']}")
            print(f"   Topic: {record_metadata.topic}")
            print(f"   Partition: {record_metadata.partition}")
            print(f"   Offset: {record_metadata.offset}")
            print()
            
        except KafkaError as e:
            print(f"❌ Failed to send message {i+1}: {e}")
        
        # Wait before sending next message
        if i < num_messages - 1:
            time.sleep(delay_seconds)
    
    # Close producer
    producer.close()
    print("✅ All messages sent!")

def main():
    """Main function"""
    import argparse
    
    parser = argparse.ArgumentParser(description='Send test order messages to Kafka')
    parser.add_argument('--count', type=int, default=10, help='Number of messages to send (default: 10)')
    parser.add_argument('--delay', type=float, default=2.0, help='Delay between messages in seconds (default: 2.0)')
    
    args = parser.parse_args()
    
    try:
        send_messages(args.count, args.delay)
    except KeyboardInterrupt:
        print("\n⏹️  Interrupted by user")
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main() 